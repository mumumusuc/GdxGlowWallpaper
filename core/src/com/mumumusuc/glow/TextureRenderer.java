package com.mumumusuc.glow;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.mumumusuc.glow.RenderRoi;

import static com.badlogic.gdx.Gdx.gl20;
import static com.badlogic.gdx.Gdx.graphics;

public class TextureRenderer implements Disposable {
    private float[] vertices;
    private short[] indices;
    private Mesh mesh;
    private ShaderProgram shader, defaultShader, userShader;
    private Matrix4 projection = new Matrix4();
    private String vert = "#version 300 es\n"
            + "layout(location=0) in vec4 a_position;\n" //
            + "layout(location=1) in vec2 a_texCoord0;\n" //
            + "uniform mat4 u_projTrans;\n" //
            + "out vec2 v_texCoords;\n" //
            + "\n" //
            + "void main()\n" //
            + "{\n" //
            + "   v_texCoords = a_texCoord0;\n" //
            + "   gl_Position =  u_projTrans * a_position;\n" //
            + "}\n";
    private String frag = "#version 300 es\n"
            + "#ifdef GL_ES\n" //
            + "#define LOWP lowp\n" //
            + "precision mediump float;\n" //
            + "#else\n" //
            + "#define LOWP \n" //
            + "#endif\n" //
            + "in vec2 v_texCoords;\n"
            + "out vec4 gl_FragColor;\n" //
            + "uniform sampler2D texture_0;\n" //
            + "void main()\n"//
            + "{\n" //
            + "  gl_FragColor = texture(texture_0, v_texCoords);\n" //
            + "}";

    TextureRenderer() {
        mesh = new Mesh(true, 8 * 3, 4,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord1")
        );
        vertices = new float[]{
                0, 0, 0, 0, 0, 0,
                0, 1, 0, 1, 0, 1,
                1, 1, 1, 1, 1, 1,
                1, 0, 1, 0, 1, 0
        };
        indices = new short[]{0, 1, 3, 2};
        mesh.setIndices(indices);
        projection.setToOrtho2D(0, 0, graphics.getWidth(), graphics.getHeight());
        defaultShader = new ShaderProgram(vert, frag);
        if (!defaultShader.isCompiled()) {
            throw new IllegalArgumentException(defaultShader.getLog());
        }
    }

    public void setProjection(Matrix4 projection) {
        projection.set(projection);
    }

    public void setProjection(int x, int y, int w, int h) {
        projection.setToOrtho2D(x, y, w, h);
    }

    public void setShader(ShaderProgram shader) {
        this.userShader = shader;
    }

    public void begin() {
        shader = userShader == null ? defaultShader : userShader;
        shader.begin();
    }

    public void end() {
        shader.end();
        shader = null;
    }

    public void setRenderRoi(int index, RenderRoi roi) {
        if (roi != null) {
            float[] uv = roi.getUV();
            vertices[2 + 2 * index] = uv[0];
            vertices[3 + 2 * index] = uv[1];
            vertices[8 + 2 * index] = uv[0];
            vertices[9 + 2 * index] = uv[3];
            vertices[14 + 2 * index] = uv[2];
            vertices[15 + 2 * index] = uv[3];
            vertices[20 + 2 * index] = uv[2];
            vertices[21 + 2 * index] = uv[1];
        } else {
            vertices[2 + 2 * index] = 0;
            vertices[3 + 2 * index] = 0;
            vertices[8 + 2 * index] = 0;
            vertices[9 + 2 * index] = 1;
            vertices[14 + 2 * index] = 1;
            vertices[15 + 2 * index] = 1;
            vertices[20 + 2 * index] = 1;
            vertices[21 + 2 * index] = 0;
        }
    }

    public void bindTexture(int index, Texture texture) {
        int handle = texture.getTextureObjectHandle();
        texture.bind(handle);
        shader.setUniformi("texture_" + index, handle);
    }

    public void render(RenderRoi roi) {
        render(roi.getRoi());
    }

    public void render(int[] roi) {
        render(roi[0], roi[1], roi[2], roi[3]);
    }

    public void render(float x, float y, float w, float h) {
        vertices[0] = x;
        vertices[1] = y;
        vertices[6] = x;
        vertices[7] = y + h;
        vertices[12] = x + w;
        vertices[13] = y + h;
        vertices[18] = x + w;
        vertices[19] = y;
        mesh.setVertices(vertices);
        shader.setUniformMatrix("u_projTrans", projection);
        //gl20.glEnable(GL20.GL_BLEND);
        //gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        mesh.render(shader, GL20.GL_TRIANGLE_STRIP);
    }

    public void render(RenderRoi roi, Texture tex, float x, float y) {
        render(roi, tex, x, y, roi.getRoiWidth(), roi.getRoiHeight());
    }

    public void render(RenderRoi roi, Texture tex, float x, float y, float w, float h) {
        setRenderRoi(0, roi);
        bindTexture(0, tex);
        render(x, y, w, h);
    }

    @Override
    public void dispose() {
        mesh.dispose();
        defaultShader.dispose();
    }

}
