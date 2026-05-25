module com.oddlabs.tools {
    requires com.oddlabs.common;
    requires org.joml;
    requires static org.jspecify;
    requires java.desktop;
    requires java.xml;
    requires org.lwjgl;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;

    exports com.oddlabs.converter;
    exports com.oddlabs.fontutil;
    exports com.oddlabs.imageutil;
}
