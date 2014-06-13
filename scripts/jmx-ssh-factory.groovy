import org.helios.jmx.remote.tunnel.*;
@CompileStatic
@InheritConstructors
class JMXSSHConnectionFactory {
	public JMXConnector connector(String host, int port, Map env) {
		JMXServiceURL jmxUrl = new JMXServiceURL("service:jmx:tunnel://$host:$port/ssh/jmxmp:");
		return JMXConnectorFactory.connect(jmxUrl, env);
	}
}
