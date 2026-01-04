package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.util.Utils;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;

/** Utilities for loading icon atlases */
final class Icons {
    private Icons() {
        // no instances
    }

	static Node loadFile(@NonNull String xml_file, @Nullable ErrorHandler error_handler) {
		URL url = Utils.makeURL(xml_file);

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(error_handler);
			Document document = builder.parse(url.openStream());
			return document.getDocumentElement();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	static @NonNull Texture loadTexture(@NonNull Node n) {
		return loadTexture(n.getAttributes().getNamedItem("texture").getNodeValue());
	}

	public static @NonNull Texture loadTexture(@NonNull String tex_file) {
				TextureFile file = new TextureFile(tex_file,
												   GL11.GL_RGBA,
												   GL11.GL_NEAREST,
												   GL11.GL_NEAREST,
												   GL12.GL_CLAMP_TO_EDGE,
												   GL12.GL_CLAMP_TO_EDGE);
				return Resources.findResource(file);
	}

	static @NonNull Node getNodeByName(@NonNull String name, @NonNull Node n) {
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeName().equals(name))
				return nl.item(i);
		}
		assert false : "Missing node: " + name;
		return null;
	}

	static int getInt(@NonNull Node n, @NonNull String key) {
		String string = n.getAttributes().getNamedItem(key).getNodeValue();
		return Integer.parseInt(string);
	}

	static @NonNull ModeIconQuads getNamedIconQuads(@NonNull Node n, @NonNull String name, @NonNull Texture texture) {
		return getIconQuads(getNodeByName(name, n), texture);
	}

    static @NonNull IconQuad getNamedIconQuad(@NonNull Node n, @NonNull String name, @NonNull Texture texture) {
		return getIconQuad(getNodeByName(name, n), texture);
	}

    static @NonNull ModeIconQuads getIconQuads(@NonNull Node n, @NonNull Texture texture) {
        return new ModeIconQuads(
                getIconQuad(getNodeByName("normal", n), texture),
                getIconQuad(getNodeByName("active", n), texture),
                getIconQuad(getNodeByName("disabled", n), texture)
        );
	}

    static @NonNull IconQuad getIconQuad(@NonNull Node n, @NonNull Texture texture) {
        Node q = getNodeByName("quad", n);
        return parseIconQuad(q, texture);
    }

    static @NonNull IconQuad parseIconQuad(@NonNull Node q, @NonNull Texture texture) {
		int left = getInt(q, "left");
		int top = getInt(q, "top");
		int right = getInt(q, "right");
		int bottom = getInt(q, "bottom");
		return new IconQuad(left/(float)texture.getWidth(),
						1f - bottom/(float)texture.getHeight(),
						right/(float)texture.getWidth(),
						1f - top/(float)texture.getHeight(),
						right - left,
						bottom - top,
						texture);
	}

    static Vector4fc getNamedColor(@NonNull Node n, @NonNull String name) {
        return getColor(getNodeByName(name, n));
    }

    static Vector4fc getColor(@NonNull Node n) {
		Node q = getNodeByName("color", n);
		byte r = (byte) getInt(q, "r");
        byte g = (byte) getInt(q, "g");
        byte b = (byte) getInt(q, "b");
        byte a = (byte) getInt(q, "a");
		return com.oddlabs.util.Color.argb4v(com.oddlabs.util.Color.argbi(r, g, b, a));
	}
}
