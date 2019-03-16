package net.xmeter.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import net.xmeter.Constants;
import net.xmeter.samplers.ConnectionSampler;

public class ConnectionSamplerUI extends AbstractSamplerGui implements Constants {
	private static final Logger logger = LoggingManager.getLoggerForClass();
	private CommonConnUI connUI = new CommonConnUI();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1666890646673145131L;

	public ConnectionSamplerUI() {
		this.init();
	}

	private void init() {
		logger.info("Initializing the UI.");
        logger.info("Initializing the UI2.");
		setLayout(new BorderLayout());
		setBorder(makeBorder());

		add(makeTitlePanel(), BorderLayout.NORTH);
		JPanel mainPanel = new VerticalPanel();
		add(mainPanel, BorderLayout.CENTER);

		mainPanel.add(connUI.createConnPanel());
		mainPanel.add(connUI.createAuthentication());
		mainPanel.add(connUI.createConnOptions());
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		ConnectionSampler sampler = (ConnectionSampler)element;
		connUI.configure(sampler);
	}

	@Override
	public TestElement createTestElement() {
		ConnectionSampler sampler = new ConnectionSampler();
		this.configureTestElement(sampler);
		connUI.setupSamplerProperties(sampler);
		return sampler;
	}

	@Override
	public String getLabelResource() {
		throw new RuntimeException();
	}

	@Override
	public String getStaticLabel() {
		return "Wildfire Connection Sampler";
	}

	@Override
	public void modifyTestElement(TestElement arg0) {
		ConnectionSampler sampler = (ConnectionSampler)arg0;
		this.configureTestElement(sampler);
		connUI.setupSamplerProperties(sampler);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		connUI.clearUI();
	}

}
