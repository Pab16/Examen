package jcolonia.daw2023.junio;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

/**
 * Gestión de acceso a un archivo de base de datos SQLite con datos de países.
 * Versión con almacenamiento de países como registros. Cada entrada se guarda
 * en un registro independiente, con lo que puede haber varios registros para un
 * mismo país que tenga varias capitales o idiomas oficiales.
 * 
 * @author <a href="dmartin.jcolonia@gmail.com">David H. Martín</a>
 * @version 4.0b (20240530)
 */
public class AccesoBD implements Closeable {
	/**
	 * Texto de la consulta SQL -SQLite- para obtener un listado general ordenado
	 * por el nombre del país. En la primera columna genera una numeración de los
	 * resultados de la consulta según el orden indicado.
	 */
	public static final String SELECT_TOTAL_POR_PAÍS = "SELECT Dense_rank() OVER (ORDER BY Name,Language ASC) Orden,Name,Capital,Language FROM country ORDER BY Name,Language";
	/**
	 * Texto de la consulta SQL -SQLite- para obtener un listado general ordenado
	 * por idiomas. En la primera columna genera una numeración de los resultados de
	 * la consulta según el orden indicado.
	 */
	public static final String SELECT_TOTAL_POR_IDIOMA = "SELECT Dense_rank() OVER (ORDER BY Language,Name ASC) Orden,Name,Capital,Language FROM country ORDER BY Language,Name";

	/** Datos de configuración del archivo de base de datos SQLite. */
	private Properties configuración;

	/** Conexión a la base de datos. */
	private Connection conexión;

	/** Nombre/ruta del archivo de la base de datos. */
	private String rutaArchivo;
	
	/** País a consultar */
	private País nuevoPais;
	
	private static int id;
	/**
	 * Recoge el nombre/ruta del archivo de la base de datos.
	 * 
	 * @param rutaArchivo el nombre/ruta referido
	 */
	public AccesoBD(String rutaArchivo) {
		conexión = null;
		configuración = null;
		this.rutaArchivo = rutaArchivo;
		id = 0;
	}

	/**
	 * Realiza la apertura de la base de datos según los datos de configuración.
	 * 
	 * @see #getConfiguración()
	 * @return la conexión realizada
	 * @throws AccesoBDExcepción si se produce alguna incidencia en todo el proceso,
	 *                           especialmente si no está disponible el controlador
	 *                           de SQLite o no se puede acceder al archivo de la
	 *                           base de datos
	 */
	public Connection getConexión() throws AccesoBDExcepción {
		Properties configuración;
		if (conexión == null) {
			configuración = getConfiguración();
			try {
				conexión = DriverManager.getConnection(configuración.getProperty("jdbc.url"));
			} catch (SQLException ex) {
				throw new AccesoBDExcepción("Error conexión SQL", ex);
			}
		}
		return conexión;
	}

	/**
	 * Facilita los datos de configuración. En caso de no estar disponibles los lee
	 * del archivo indicado.
	 * 
	 * @see #leerConfiguración(String)
	 * @return los datos de configuración
	 * @throws AccesoBDExcepción si se produce alguna incidencia en todo el proceso
	 */
	private Properties getConfiguración() throws AccesoBDExcepción {
		if (configuración == null) {
			configuración = leerConfiguración(rutaArchivo);
		}
		return configuración;
	}

	/**
	 * Si la conexión está abierta, cierra la conexión.
	 * 
	 * @throws IOException si se produce alguna incidencia
	 */
	@Override
	public void close() throws IOException {
		try {
			if (conexión != null && !conexión.isClosed()) {
				conexión.close();
			}
		} catch (SQLException ex) {
			throw new IOException("Error al cerrar conexión SQL", ex);
		}
	}

	/**
	 * Realiza la consulta indicada a la base de datos, crea los países y los agrupa
	 * en una colección. Evita agregar países con nombres repetidos; en tal caso
	 * agrega el idioma al país existente.
	 * 
	 * @param textoSQL el texto de la consulta a realizar.
	 * @return la colección de países obtenidos
	 * @throws AccesoBDExcepción si se produce cualquier incidencia, especialmente
	 *                           en el transcurso de la recogida de los países.
	 * 
	 * @see #SELECT_TOTAL_POR_PAÍS
	 * @see #SELECT_TOTAL_POR_IDIOMA
	 */
	public RelaciónPaíses consultarPaíses(String textoSQL) throws AccesoBDExcepción {
		RelaciónPaíses conjunto = null;
		País elemento;

		ResultSet resultado;

		int número;
		String nombrePaís, nombreCapital, nombreIdioma;

		try (Connection conexión = getConexión(); Statement consultaSQL = conexión.createStatement();) {
			resultado = consultaSQL.executeQuery(textoSQL);

			conjunto = new RelaciónPaíses();
			while (resultado.next()) {
				número = resultado.getInt("Orden");
				nombrePaís = resultado.getString("Name");
				nombreCapital = resultado.getString("Capital");
				nombreIdioma = resultado.getString("Language");

				elemento = new País(número, nombrePaís, nombreCapital, nombreIdioma);
				conjunto.añadir(elemento);
			}
		} catch (AccesoBDExcepción ex) { // Cerrar y propagar
			throw ex;
		} catch (SQLException ex) { // Encapsular
			throw new AccesoBDExcepción("Error de lectura SQL", ex);
		}

		if (conjunto.getNúmElementos() == 0) {
			throw new AccesoBDExcepción("Carga infructuosa, 0 elementos");
		}

		return conjunto;
	}

	/**
	 * Crea un típico archivo de configuración para acceso a una base de datos
	 * –SQLite– en el formato XML de
	 * {@link Properties#storeToXML(java.io.OutputStream, String)}.
	 * 
	 * @param textoRutaConfig      el nombre/ruta del archivo de configuración
	 * @param textoRutaArchivoBD   el nombre/ruta del archivo de la base de datos
	 *                             SQLite
	 * @param sobrescribirAnterior indica si se permite sobrescribir un archivo de
	 *                             configuración previo existente con ese mismo
	 *                             nombre
	 * @throws AccesoBDExcepción si se produce cualquier incidencia en la grabación
	 *                           del archivo; en particular si el archivo existe y
	 *                           no se ha permitido la sobrescritura del mismo.
	 */
	public static void grabarConfiguración(final String textoRutaConfig, final String textoRutaArchivoBD,
			final boolean sobrescribirAnterior) throws AccesoBDExcepción {
		Properties configuraciónModelo;

		Path rutaConfig, rutaBD;
		rutaConfig = Path.of(textoRutaConfig);
		rutaBD = Path.of(textoRutaArchivoBD);

		if (!sobrescribirAnterior && Files.exists(rutaConfig)) {
			throw new AccesoBDExcepción("Archivo de configuración ya existe: " + rutaConfig.toString());
		}

		configuraciónModelo = new Properties();

		configuraciónModelo.setProperty("jdbc.url", "jdbc:sqlite:" + rutaBD.toString());
		configuraciónModelo.setProperty("jdbc.user", "");
		configuraciónModelo.setProperty("jdbc.password", "🙁");
		configuraciónModelo.setProperty("jdbc.codificación", "UTF-8");

		try (FileOutputStream out = new FileOutputStream(rutaConfig.toFile())) {
			configuraciónModelo.storeToXML(out, "Configuración BD", "UTF-8");
		} catch (FileNotFoundException ex) {
			throw new AccesoBDExcepción("Error de archivo", ex);
		} catch (IOException ex) {
			throw new AccesoBDExcepción("Error E/S", ex);
		}
	}

	/**
	 * Carga los datos de configuración de un archivo en el formato XML de
	 * {@link Properties#storeToXML(java.io.OutputStream, String)}.
	 * 
	 * @param textoRutaConfig el nombre/ruta del archivo de configuración
	 * @return los datos de configuración
	 * @throws AccesoBDExcepción si se produce cualquier incidencia en el acceso y
	 *                           lectura del archivo de configuración.
	 */
	public static Properties leerConfiguración(final String textoRutaConfig) throws AccesoBDExcepción {
		Properties configuraciónLeída;

		Path rutaConfig;
		rutaConfig = Path.of(textoRutaConfig);

		if (!Files.exists(rutaConfig)) {
			throw new AccesoBDExcepción("Archivo de configuración no existe: " + rutaConfig.toString());
		}

		configuraciónLeída = new Properties();

		try (FileInputStream in = new FileInputStream(rutaConfig.toFile())) {
			configuraciónLeída.loadFromXML(in);
		} catch (FileNotFoundException ex) {
			throw new AccesoBDExcepción("Error de archivo", ex);
		} catch (IOException ex) {
			throw new AccesoBDExcepción("Error E/S", ex);
		}
		return configuraciónLeída;
	}
	
	/**
	 * Busca un pais en la base de datos y devuelve sus características.
	 * @param pais
	 * @throws BDException 
	 */
	public void consultaPais(String pais) throws AccesoBDExcepción {
		String fuente = "jdbc:sqlite:world2.db";
		String consulta = String.format("SELECT * from Country WHERE Name LIKE ?");
		
		try(	Connection conexión = DriverManager.getConnection(fuente);
				Statement sentenciaSQL = conexión.createStatement();
				PreparedStatement preparaciónSQL = conexión.prepareStatement(consulta);
				){
			
			preparaciónSQL.setString(1, pais);
			ResultSet loteDatos = preparaciónSQL.executeQuery();
			sentenciaSQL.setQueryTimeout(5);
			rellenarPais(loteDatos);
			

		}catch(SQLException ex) {
			System.err.printf("Error: %s \n--%s--\n%s",
					"No se ha creado la base de datos SQLite",
					ex.getLocalizedMessage());
			ex.getStackTrace();
			System.exit(1);
		}	
	}
	
	/**
	 * Rellena la lista con un conjunto de características de un país: nombre, capital e idioma 
	 * 
	 * @param loteDatos
	 * @throws BDException
	 * @throws SQLException
	 */
	public void rellenarPais(ResultSet resultado) throws AccesoBDExcepción, SQLException {
		String nombre = "", capital = "", idioma = "";
		int número = ++id;
		while (resultado.next()) {
			nombre = resultado.getString("Name");
			capital = resultado.getString("Capital");
			idioma += resultado.getString("Language");
			idioma += " ";
		}
		
		if(nombre != "") {
			nuevoPais = new País(número, nombre, capital, idioma);
		}else {
			throw new AccesoBDExcepción("¡País no encontrado!");
		}
	}
	
	public País getPaís() {
		return nuevoPais;
	}

}
