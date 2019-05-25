package net.xmeter.gui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.xmeter.samplers.SendMessageSampler;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledChoice;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import net.xmeter.Constants;

public class SendMessageSamplerUI extends AbstractSamplerGui implements Constants, ChangeListener {
	private static final Logger logger = LoggingManager.getLoggerForClass();
	private CommonConnUI connUI = new CommonConnUI();
	/**
	 * 
	 */
	private static final long serialVersionUID = 2479085966683186422L;

	private JLabeledChoice conversationTypeChoice = new JLabeledChoice("ConversationType:", new String[] { CONV_TYPE_SINGLE, CONV_TYPE_GROUP }, true, false);
	private final JLabeledTextField target = new JLabeledTextField("Target:");
//	private JCheckBox timestamp = new JCheckBox("Add timestamp in payload");

	private final JSyntaxTextArea sendMessage = JSyntaxTextArea.getInstance(10, 50);
	private final JTextScrollPane messagePanel = JTextScrollPane.getInstance(sendMessage);
//	private JLabeledTextField stringLength = new JLabeledTextField("Length:");

	public SendMessageSamplerUI() {
		init();
	}

	private void init() {
		logger.info("Initializing the UI.");
		setLayout(new BorderLayout());
		setBorder(makeBorder());

		add(makeTitlePanel(), BorderLayout.NORTH);
		JPanel mainPanel = new VerticalPanel();
		add(mainPanel, BorderLayout.CENTER);

		mainPanel.add(connUI.createConnPanel());
		mainPanel.add(connUI.createAuthentication());
		mainPanel.add(connUI.createConnOptions());

		mainPanel.add(createPubOption());
		mainPanel.add(createPayload());
	}

	private JPanel createPubOption() {
		JPanel optsPanelCon = new VerticalPanel();
		optsPanelCon.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Send message options"));

		conversationTypeChoice.addChangeListener(this);

		JPanel optsPanel = new HorizontalPanel();
		optsPanel.add(conversationTypeChoice);
		optsPanel.add(target);
		optsPanelCon.add(optsPanel);

		return optsPanelCon;
	}

	private JPanel createPayload() {
		JPanel optsPanelCon = new VerticalPanel();
		optsPanelCon.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Message Content"));

		JPanel horizon2 = new VerticalPanel();
		messagePanel.setVisible(true);
		horizon2.add(messagePanel);

		optsPanelCon.add(horizon2);
		return optsPanelCon;
	}

	@Override
	public String getStaticLabel() {
		return "Wildfire Send Message Sampler";
	}

	@Override
	public void stateChanged(ChangeEvent e) {
//		if(e.getSource() == this.messageTypes) {
//			int selectedIndex = this.messageTypes.getSelectedIndex();
//			if(selectedIndex == 0 || selectedIndex == 1) {
//				stringLength.setVisible(false);
//				messagePanel.setVisible(true);
//			} else if(selectedIndex == 2) {
//				messagePanel.setVisible(false);
//				stringLength.setVisible(true);
//			} else {
//				logger.info("Unknown message type.");
//			}
//		}
	}

	@Override
	public String getLabelResource() {
		return "";
	}

	@Override
	public TestElement createTestElement() {
        logger.info("createTestElement called.");
		SendMessageSampler sampler = new SendMessageSampler();
		this.setupSamplerProperties(sampler);
        connUI.setupSamplerProperties(sampler);
        return sampler;
	}

	@Override
	public void configure(TestElement element) {
        logger.info("configure called.");
		super.configure(element);
		SendMessageSampler sampler = (SendMessageSampler) element;
		
		connUI.configure(sampler);
//		if(sampler.getQOS().trim().indexOf(JMETER_VARIABLE_PREFIX) == -1){
        if ("1".equals(sampler.getConvType())) {
            this.conversationTypeChoice.setSelectedIndex(1);
        } else {
            this.conversationTypeChoice.setSelectedIndex(0);
        }
//		} else {
//			this.conversationTypeChoice.setText(sampler.getQOS());
//		}
		
		this.target.setText(sampler.getTarget());
//		this.timestamp.setSelected(sampler.isAddTimestamp());
//		if(MESSAGE_TYPE_STRING.equalsIgnoreCase(sampler.getMessageType())) {
//			this.messageTypes.setSelectedIndex(0);
//			this.messagePanel.setVisible(true);
//		} else if(MESSAGE_TYPE_HEX_STRING.equalsIgnoreCase(sampler.getMessageType())) {
//			this.messageTypes.setSelectedIndex(1);
//		} else if(MESSAGE_TYPE_RANDOM_STR_WITH_FIX_LEN.equalsIgnoreCase(sampler.getMessageType())) {
//			this.messageTypes.setSelectedIndex(2);
//		}
//
//		stringLength.setText(String.valueOf(sampler.getMessageLength()));
		sendMessage.setText(sampler.getMessage());
	}

	@Override
	public void modifyTestElement(TestElement arg0) {
        logger.info("modifyTestElement called.");
		SendMessageSampler sampler = (SendMessageSampler) arg0;
		this.setupSamplerProperties(sampler);
		connUI.setupSamplerProperties(sampler);
	}

	private void setupSamplerProperties(SendMessageSampler sampler) {
		this.configureTestElement(sampler);
		connUI.setupSamplerProperties(sampler);

		sampler.setTarget(this.target.getText());
	    sampler.setConvType(this.conversationTypeChoice.getSelectedIndex() + "");
		sampler.setMessage(this.sendMessage.getText());
	}

	@Override
	public void clearGui() {
        logger.info("clearGui called.");
		super.clearGui();
		connUI.clearUI();
		connUI.connNamePrefix.setText(DEFAULT_CONN_PREFIX_FOR_PUB);
		this.target.setText(DEFAULT_TARGET);
		this.conversationTypeChoice.setSelectedIndex(0);
		this.sendMessage.setText("");
	}
}
