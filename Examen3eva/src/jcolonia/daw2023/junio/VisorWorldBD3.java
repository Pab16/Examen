package jcolonia.daw2023.junio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * Aplicación de ventanas con una única ventana principal y una tabla de datos
 * central utilizada para consultar una base de datos de países.
 * 
 * @author <a href="dmartin.jcolonia@gmail.com">David H. Martín</a>
 * @version 4.0 (20250530)
 */
public class VisorWorldBD3 extends JFrame {
	/** Número de serie, asociado a la versión de la clase. */
	private static final long serialVersionUID = 20240530000L;
	/** El panel general exterior. */
	private JPanel panelGeneral;
	/** El panel general que agrupa todos los posibles paneles de cada pestaña. */
	private JTabbedPane panelPestañas;
	/** El panel de contenido central de la única pestaña, con márgenes. */
	private JPanel panelPrincipal;
	/** El panel con barras de desplazamiento para la tabla. */
	private JScrollPane panelTablaDeslizante;
	/** La tabla central para mostrar los datos de los países. */
	private JTable tablaPaíses;
	/** El modelo de datos de la tabla de países. */
	private ModeloTablaPaíses modeloPaíses;
	/** El panel inferior donde se sitúan los botones principales de la pestaña. */
	private JPanel panelBotones;
	/** El botón de insertar, abajo a la derecha. */
	private JButton botónInsertar;

	/** Control general asociado a la aplicación/ventana. */
	private ControlWorldBD control;

	/** Indicador de preparación de ventana finalizada. */
	private boolean ventanaPreparada;
	private JTextField textoEstado;
	private JMenuBar barraMenu;
	private JMenu menuAyuda;
	private JMenuItem itemMostrarCreditos;
	private JMenu menuArchivo;
	private JMenuItem itemSalir;
	private JFrame ventanaPrincipal;
	private JButton botonConsulta;
	private JTextField textoPais;

	/**
	 * Lanza la aplicación. Establece la apariencia general de la ventana y registra
	 * el lanzador.
	 * 
	 * @param argumentos Opciones en línea de órdenes –no se usa–.
	 * 
	 */
	public static void main(String[] argumentos) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			/**
			 * Crea la ventana y la hace visible.
			 */
			public void run() {
				try {
					VisorWorldBD3 ventana = new VisorWorldBD3();
					ventana.setVisible(true);
					ventana.ventanaPreparada = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Crea la aplicación e inicializa los componentes de la ventana.
	 */
	public VisorWorldBD3() {
		ventanaPreparada = false;
		ventanaPrincipal = this;
		control = new ControlWorldBD();
		initialize();
	}

	/**
	 * Inicializa los componentes de la ventana.
	 */
	private void initialize() {
		setTitle("Ventana países");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 573, 411);
		setJMenuBar(getMenuBar_1());
		panelGeneral = new JPanel();
		panelGeneral.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(panelGeneral);
		panelGeneral.setLayout(new BorderLayout(0, 0));
		panelGeneral.add(getPanelPestañas(), BorderLayout.CENTER);
		panelGeneral.add(getTextoEstado(), BorderLayout.SOUTH);
	}

	/**
	 * Localiza –o inicializa si no se ha creado todavía– el panel general exterior,
	 * para las pestañas.
	 * 
	 * @return el panel indicado
	 */
	private JTabbedPane getPanelPestañas() {
		if (panelPestañas == null) {
			panelPestañas = new JTabbedPane(JTabbedPane.TOP);
			panelPestañas.setBorder(new EmptyBorder(10, 10, 10, 10));
			panelPestañas.addTab("Listado", null, getPanelPrincipal(), null);

		}
		return panelPestañas;
	}

	/**
	 * Localiza –o inicializa si no se ha creado todavía– el panel de contenido
	 * central de la única pestaña, con márgenes.
	 * 
	 * @return el panel indicado
	 */
	private JPanel getPanelPrincipal() {
		if (panelPrincipal == null) {
			panelPrincipal = new JPanel();
			panelPrincipal.setBorder(new EmptyBorder(10, 0, 0, 0));
			panelPrincipal.setLayout(new BorderLayout(0, 10));

			panelPrincipal.add(getPanelBotones(), BorderLayout.SOUTH);
			panelPrincipal.add(getPanelTablaDeslizante(), BorderLayout.CENTER);
		}
		return panelPrincipal;
	}

	/**
	 * Localiza –o inicializa si no se ha creado todavía– el panel con barras de
	 * desplazamiento para la tabla.
	 * 
	 * @return el panel indicado
	 */
	private JScrollPane getPanelTablaDeslizante() {
		if (panelTablaDeslizante == null) {
			panelTablaDeslizante = new JScrollPane();
			panelTablaDeslizante.setBackground(Color.ORANGE);
			panelTablaDeslizante.setViewportView(getTablaPaíses());
		}
		return panelTablaDeslizante;
	}

	/**
	 * Localiza –o inicializa si no se ha creado todavía– la tabla central para
	 * mostrar los datos de los países.
	 * 
	 * @return la tabla indicada
	 */
	private JTable getTablaPaíses() {
		if (tablaPaíses == null) {
			tablaPaíses = new JTable();
			tablaPaíses.setFillsViewportHeight(true);
			tablaPaíses.setShowVerticalLines(true);
			tablaPaíses.setShowHorizontalLines(true);
			tablaPaíses.setGridColor(new Color(255, 228, 181));
			tablaPaíses.setBorder(new LineBorder(new Color(0, 0, 0)));
			tablaPaíses.setAutoCreateRowSorter(true);
			tablaPaíses.setModel(getModeloPaíses());
		}
		return tablaPaíses;
	}

	/**
	 * Localiza –o inicializa si no se ha creado todavía– el modelo de datos de la
	 * tabla de países.
	 * 
	 * @return el modelo de datos indicado
	 */
	public ModeloTablaPaíses getModeloPaíses() {
		if (modeloPaíses == null) {
			modeloPaíses = new ModeloTablaPaíses();
		}
		return modeloPaíses;
	}

	/**
	 * Localiza –o inicializa si no se ha creado todavía– el panel inferior donde se
	 * sitúan los botones principales de la pestaña.
	 * 
	 * @return el panel indicado
	 */
	private JPanel getPanelBotones() {
		if (panelBotones == null) {
			panelBotones = new JPanel();
			panelBotones.setBorder(new EmptyBorder(0, 0, 0, 0));
			panelBotones.setLayout(new GridLayout(0, 1, 0, 0));
			panelBotones.add(getTextoPais());
			panelBotones.add(getBotonConsulta());
			panelBotones.add(getBotónInsertar());
		}
		return panelBotones;
	}

	/**
	 * Localiza –o inicializa si no se ha creado todavía– el botón de insertar,
	 * abajo a la derecha.
	 * 
	 * @return el botón indicado
	 */
	private JButton getBotónInsertar() {
		if (botónInsertar == null) {
			botónInsertar = new JButton("Insertar");
			botónInsertar.addActionListener(new BotónInsertarActionListener());
			botónInsertar.setMnemonic(KeyEvent.VK_I);
		}
		return botónInsertar;
	}

	/**
	 * Monitor de eventos para el botón de insertar.
	 */
	private class BotónInsertarActionListener implements ActionListener {
		/**
		 * Carga la tabla con los datos de la base de datos de países.
		 * 
		 * @param ev el evento causante
		 */
		public void actionPerformed(ActionEvent ev) {
			ModeloTablaPaíses modelo = getModeloPaíses();
			String mensaje;
			try {
			control.cargarDatos();
			mensaje = String.format("Se han cargado %d datos.", control.consultarTamaño());
			control.sincronizarListaPaíses(modelo);
			mostrarEstado(mensaje);
			} catch(AccesoBDExcepción ex) {
				mostrarAviso(ex.getMessage());
			}
		}
	}
	/**
	 * Se mostrará un popup con la información del autor y la fecha.
	 */
	private class ItemMostrarCreditosActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			mostrarPopup("(c) Pablo Arranz 2024");
		}
	}
	
	/**
	 * Al pulsar el botón se cerrará la ventana.
	 */
	private class ItemSalirActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			ventanaPrincipal.dispose();
		}
	}
	
	
	private class BotonConsultaActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				control.consultaPaís(modeloPaíses, getTextoPais().getText());
				mostrarEstado("");
			} catch (IOException ex) {
				mostrarAviso(ex.getMessage());
			} catch (AccesoBDExcepción ex) {
				mostrarEstado(ex.getMessage());
			} catch (SQLException ex) {
				String mensaje = String.format("Error: %s \n--%s--\n%s",
						"No se ha creado la base de datos SQLite",
						ex.getLocalizedMessage());
				mostrarAviso(mensaje);
			}
		}
	}
	
	/**
	 * Barra de texto dónde se mostrará información relvante
	 * 
	 * @return
	 */
	private JTextField getTextoEstado() {
		if (textoEstado == null) {
			textoEstado = new JTextField();
			textoEstado.setHorizontalAlignment(SwingConstants.CENTER);
			textoEstado.setColumns(10);
		}
		return textoEstado;
	}
	
	/**
	 * Mostrará el mensaje dado por la barra de estado. 
	 * 
	 * @param mensaje
	 */
	public void mostrarEstado(String mensaje) {
		getTextoEstado().setForeground((new Color(0, 0, 0)));
		getTextoEstado().setText(mensaje);
	}
	
	/**
	 * Mostrará el aviso por la barra de estado.
	 * 
	 * @param mensaje
	 */
	public void mostrarAviso(String mensaje) {
		getTextoEstado().setForeground((new Color(255, 0, 0)));
		getTextoEstado().setText(mensaje);
	}
	
	/**
	 * Saca un popup por pantalla con el mensaje dado.
	 * 
	 * @param mensaje
	 */
	public void mostrarPopup(String mensaje) {
		JOptionPane.showMessageDialog(getPanelPestañas(), mensaje, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
	/**
	 * Barra de menú.
	 * 
	 * @return
	 */
	private JMenuBar getMenuBar_1() {
		if (barraMenu == null) {
			barraMenu = new JMenuBar();
			barraMenu.add(getMenuAyuda());
			barraMenu.add(getMenuArchivo());
		}
		return barraMenu;
	}
	
	/**
	 * Submenú de ayuda del menú.
	 * 
	 * @return
	 */
	private JMenu getMenuAyuda() {
		if (menuAyuda == null) {
			menuAyuda = new JMenu("Ayuda");
			menuAyuda.setMnemonic(KeyEvent.VK_U);
			menuAyuda.add(getItemMostrarCreditos());
		}
		return menuAyuda;
	}
	
	/**
	 * Opción de mostrar los créditos del submenú.
	 * @return
	 */
	private JMenuItem getItemMostrarCreditos() {
		if (itemMostrarCreditos == null) {
			itemMostrarCreditos = new JMenuItem("Créditos");
			itemMostrarCreditos.addActionListener(new ItemMostrarCreditosActionListener());
		}
		return itemMostrarCreditos;
	}
	
	/**
	 * Segundo submenú
	 * 
	 * @return
	 */
	private JMenu getMenuArchivo() {
		if (menuArchivo == null) {
			menuArchivo = new JMenu("Archivo");
			menuArchivo.setMnemonic(KeyEvent.VK_A);
			menuArchivo.add(getItemSalir());
		}
		return menuArchivo;
	}
	
	/**
	 * Item del submenú "Archivo" que te permitirá cerrar la ventana.
	 * 
	 * @return
	 */
	private JMenuItem getItemSalir() {
		if (itemSalir == null) {
			itemSalir = new JMenuItem("Salir del programa");
			itemSalir.addActionListener(new ItemSalirActionListener());
		}
		return itemSalir;
	}
	
	/**
	 * Botón de consulta de un país.
	 * 
	 * @return
	 */
	private JButton getBotonConsulta() {
		if (botonConsulta == null) {
			botonConsulta = new JButton("Buscar país");
			botonConsulta.addActionListener(new BotonConsultaActionListener());
		}
		return botonConsulta;
	}
	
	/**
	 * Zona donde escribir el país a buscar.
	 * 
	 * @return
	 */
	private JTextField getTextoPais() {
		if (textoPais == null) {
			textoPais = new JTextField();
			textoPais.setColumns(10);
		}
		return textoPais;
	}
}
