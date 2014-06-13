@CompileStatic
@InheritConstructors
class JMXMPConnectionFactory {
	public JMXConnector connector(String host, int port) {
		return JMXConnectorFactory.newJMXConnector(new JMXServiceURL("service:jmx:jmxmp://$host:$port"), null);
	}
}

