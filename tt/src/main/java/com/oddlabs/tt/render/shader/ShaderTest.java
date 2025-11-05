package com.oddlabs.tt.render.shader;

public final class ShaderTest {
	private static ShaderProgram testShader;
	private static ShaderRenderer testRenderer;
	private static boolean initialized = false;
	
	private ShaderTest() {}
	
	public static void initialize() {
		if (initialized) {
			return;
		}
		
		try {
			testShader = FixedFunctionShader.create();
			testRenderer = new ShaderRenderer(testShader);
			initialized = true;
			IO.println("Shader system initialized successfully");
		} catch (Exception e) {
			System.err.println("Shader initialization failed: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void renderTestTriangle() {
		if (!initialized) {
			return;
		}
		
		try {
			testShader.use();
			testShader.setUniform(FixedFunctionShader.Uniforms.ENABLE_LIGHTING, 0);
			testShader.setUniform(FixedFunctionShader.Uniforms.ENABLE_TEXTURE, 0);
			
			testRenderer.getProjectionStack().loadIdentity();
			testRenderer.getModelViewStack().loadIdentity();
			
			testRenderer.begin();
			
			testRenderer.vertex(0.0f, 0.5f, 0.0f,
			                   0.0f, 0.0f, 1.0f,
			                   1.0f, 0.0f, 0.0f, 1.0f,
			                   0.5f, 1.0f);
			
			testRenderer.vertex(-0.5f, -0.5f, 0.0f,
			                   0.0f, 0.0f, 1.0f,
			                   0.0f, 1.0f, 0.0f, 1.0f,
			                   0.0f, 0.0f);
			
			testRenderer.vertex(0.5f, -0.5f, 0.0f,
			                   0.0f, 0.0f, 1.0f,
			                   0.0f, 0.0f, 1.0f, 1.0f,
			                   1.0f, 0.0f);
			
			testRenderer.end();
			
			ShaderProgram.unbind();
		} catch (Exception e) {
			System.err.println("Shader rendering failed: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void cleanup() {
		if (testRenderer != null) {
			testRenderer.cleanup();
		}
		initialized = false;
	}
	
	public static boolean isInitialized() {
		return initialized;
	}
}
