package net.xmeter.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jorphan.gui.JLabeledTextField;

import net.xmeter.Constants;
import net.xmeter.samplers.AbstractMQTTSampler;

public class CommonConnUI implements ChangeListener, ActionListener, Constants{
	private final JLabeledTextField serverAddr = new JLabeledTextField("Server name or IP:");
	private final JLabeledTextField serverPort = new JLabeledTextField("Port number:", 5);
	private JCheckBox connShared = new JCheckBox("Share conn in thread");
	private final JLabeledTextField timeout = new JLabeledTextField("Timeout(s):", 5);
	
	private final JLabeledTextField userNameAuth = new JLabeledTextField("User name:");
    private JCheckBox userNameSuffix = new JCheckBox("Add random suffix for user name");

	public final JLabeledTextField connNamePrefix = new JLabeledTextField("ClientId:", 8);
	private JCheckBox connNameSuffix = new JCheckBox("Add random suffix for ClientId");
	
	private final JLabeledTextField connKeepAlive = new JLabeledTextField("Keep alive(s):", 4);
	
	private final JLabeledTextField connKeeptime = new JLabeledTextField("Connection keep time(s):", 4);
	
	private final JLabeledTextField connAttmptMax = new JLabeledTextField("Connect attampt max:", 0);
	private final JLabeledTextField reconnAttmptMax = new JLabeledTextField("Reconnect attampt max:", 0);
	
	public JPanel createConnPanel() {
		JPanel con = new HorizontalPanel();
		
		JPanel connPanel = new HorizontalPanel();
		connPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "MQTT connection"));
		connPanel.add(serverAddr);
		connPanel.add(serverPort);
		connPanel.add(connShared);
		
		JPanel timeoutPannel = new HorizontalPanel();
		timeoutPannel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Timeout"));
		timeoutPannel.add(timeout);

		con.add(connPanel);
		con.add(timeoutPannel);
		return con;
	}
	
	public JPanel createConnOptions() {
		JPanel optsPanelCon = new VerticalPanel();
		optsPanelCon.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Connection options"));
		
		JPanel optsPanel0 = new HorizontalPanel();
		optsPanel0.add(connNamePrefix);
		optsPanel0.add(connNameSuffix);
		connNameSuffix.setSelected(true);
		optsPanelCon.add(optsPanel0);
		
		JPanel optsPanel1 = new HorizontalPanel();
		optsPanel1.add(connKeepAlive);
		optsPanel1.add(connKeeptime);
		optsPanelCon.add(optsPanel1);
		
		optsPanel1.add(connAttmptMax);
		optsPanel1.add(reconnAttmptMax);
		optsPanelCon.add(optsPanel1);
		
		return optsPanelCon;
	}
	
	public JPanel createAuthentication() {
		JPanel optsPanelCon = new VerticalPanel();
		optsPanelCon.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "User authentication"));
		
		JPanel optsPanel = new HorizontalPanel();
		optsPanel.add(userNameAuth);
		optsPanel.add(userNameSuffix);
		optsPanelCon.add(optsPanel);
		
		return optsPanelCon;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
	}
	
	public void configure(AbstractMQTTSampler sampler) {
		serverAddr.setText(sampler.getServer());
		serverPort.setText(sampler.getPort());
		connShared.setSelected(sampler.isConnectionShare());
		if(!sampler.isConnectionShareShow()) {
			connShared.setVisible(false);
		}
		timeout.setText(sampler.getConnTimeout());

		userNameAuth.setText(sampler.getUserNameProperty());
        userNameSuffix.setSelected(sampler.isUserNamePrefix());
		
		connNamePrefix.setText(sampler.getConnPrefix());
		if(sampler.isClientIdSuffix()) {
			connNameSuffix.setSelected(true);
		} else {
			connNameSuffix.setSelected(false);
		}
		
		connKeepAlive.setText(sampler.getConnKeepAlive());
		connKeeptime.setText(sampler.getConnKeepTime());
		if(!sampler.isKeepTimeShow()) {
			connKeeptime.setVisible(false);
		}
		connAttmptMax.setText(sampler.getConnAttamptMax());
		reconnAttmptMax.setText(sampler.getConnReconnAttamptMax());
	}
	
	
	public void setupSamplerProperties(AbstractMQTTSampler sampler) {
		sampler.setServer(serverAddr.getText());
		sampler.setPort(serverPort.getText());
		sampler.setConnectionShare(connShared.isSelected());
		sampler.setConnTimeout(timeout.getText());
		sampler.setConnectionShare(connShared.isSelected());

		sampler.setUserNameAuth(userNameAuth.getText());
		sampler.setUserNamePrefix(userNameSuffix.isSelected());
		
		sampler.setConnPrefix(connNamePrefix.getText());
		sampler.setClientIdSuffix(connNameSuffix.isSelected());
		
		sampler.setConnKeepAlive(connKeepAlive.getText());
		sampler.setConnKeepTime(connKeeptime.getText());
		sampler.setConnAttamptMax(connAttmptMax.getText());
		sampler.setConnReconnAttamptMax(reconnAttmptMax.getText());
	}
	
	public static int parseInt(String value) {
		if(value == null || "".equals(value.trim())) {
			return 0;
		}
		return Integer.parseInt(value);
	}
	
	public void clearUI() {
		serverAddr.setText(DEFAULT_SERVER);
		serverPort.setText(DEFAULT_PORT);
		connShared.setSelected(DEFAULT_CONNECTION_SHARE);
		timeout.setText(DEFAULT_CONN_TIME_OUT);

		
		userNameAuth.setText("");
		userNameSuffix.setSelected(true);
		
		connNamePrefix.setText(DEFAULT_CONN_PREFIX_FOR_CONN);
		connNameSuffix.setSelected(true);
		
		connKeepAlive.setText(DEFAULT_CONN_KEEP_ALIVE);
		connKeeptime.setText(DEFAULT_CONN_KEEP_TIME);
		connAttmptMax.setText(DEFAULT_CONN_ATTAMPT_MAX);
		reconnAttmptMax.setText(DEFAULT_CONN_RECONN_ATTAMPT_MAX);
	}
}
