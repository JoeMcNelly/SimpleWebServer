package jarloader;

public class JarLoader extends MultiClassLoader

{
	private JarResources jarResources;

	public JarLoader(String jarName) {
		// Create the JarResource and suck in the jar file.
		jarResources = new JarResources(jarName);
	}

	protected byte[] loadClassBytes(String className) {
		// Support the MultiClassLoader's class name munging facility.
		className = formatClassName(className);
		// Attempt to get the class data from the JarResource.
		return (jarResources.getResource(className));
	}
}



// Code courtesy of http://www.javaworld.com/article/2077572/learn-java/java-tip-70--create-objects-from-jar-files-.html

