package com.mumumusuc.glow;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.ModelInstanceParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import static com.badlogic.gdx.Gdx.app;
import static com.badlogic.gdx.Gdx.files;
import static com.badlogic.gdx.Gdx.gl;
import static com.badlogic.gdx.Gdx.graphics;
import static com.badlogic.gdx.Gdx.input;

public class Glow extends ApplicationAdapter {
    static final String TAG = Glow.class.getSimpleName();
    static final String SHADER_VERT = "shader/blur.vert";
    static final String SHADER_FRAG = "shader/blur.frag";
    static final String RENDER_VERT = "shader/render.vert";
    static final String RENDER_FRAG = "shader/render.frag";
    static final String SAMPLER_FRAG = "shader/sampler.frag";
    static final String MODEL = "models/godzilla.g3db";
    static final String MODEL_ADD = "models/godzilla_add.g3db";
    static final String Texture_D = "models/Godzilla_D.tga";
    static final String Texture_E = "models/Godzilla_E.tga";
    static final String Texture_N = "models/Godzilla_N.tga";
    static final String Texture_S = "models/Godzilla_S.tga";
    static final String EFFECT = "effects/beam.pfx";
    static final int BUFFER_WIDTH = 256;
    static final int BUFFER_HEIGHT = 256;
    float exposure = 0.2f, enhance = 1.5f, time = 0;

    PerspectiveCamera camera;
    CameraInputController controller;
    AssetManager assets;
    Environment environment;
    ModelBatch modelBatch;
    Array<ModelInstance> instances = new Array<ModelInstance>();
    Array<ModelInstance> renderables = new Array<ModelInstance>();
    PointLight light;
    ParticleEffect effect;
    ParticleSystem particleSystem;
    ShaderProgram samplerShader, blurShader, renderShader;
    TextureRenderer mesh;
    RenderRoi screenRegion, blurRegion;
    /**
     * render contents to this buffer(2), use MRT is supported
     */
    RenderBuffer screenBuffers;
    /**
     * blur contents to this buffer(2)
     */
    RenderBuffer blurBuffers;

    @Override
    public void create() {
        //graphics.setContinuousRendering(false);
        samplerShader = new ShaderProgram(files.internal(SHADER_VERT), files.internal(SAMPLER_FRAG));
        if (!samplerShader.isCompiled()) {
            throw new IllegalArgumentException(samplerShader.getLog());
        }
        blurShader = new ShaderProgram(files.internal(SHADER_VERT), files.internal(SHADER_FRAG));
        if (!blurShader.isCompiled()) {
            throw new IllegalArgumentException(blurShader.getLog());
        }
        renderShader = new ShaderProgram(files.internal(RENDER_VERT), files.internal(RENDER_FRAG));
        if (!renderShader.isCompiled()) {
            throw new IllegalArgumentException(renderShader.getLog());
        }

        camera = new PerspectiveCamera(67, graphics.getWidth(), graphics.getHeight());
        camera.position.set(-10, 7, 10);
        camera.lookAt(0, 2, 0);
        camera.far = 1000;
        camera.near = 1;
        camera.update();
        controller = new CameraInputController(camera);
        Gdx.input.setInputProcessor(controller);

        environment = new Environment();
        float rgb = .0f;
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, rgb, rgb, rgb, 1f));
        environment.set(new ColorAttribute(ColorAttribute.Fog, Color.BLACK));
        light = new PointLight().set(Color.WHITE, new Vector3(-.1f, 6f, -3f), 8f);
        light.setPosition(-0.006f, 7.27380f, 4.0610f);
        environment.add(light);
        //environment.add(new DirectionalLight().set(Color.WHITE, 1, 1, -1));
        assets = new AssetManager();
        modelBatch = new ModelBatch(new CartoonShaderProvider(null));
        mesh = new TextureRenderer();
        int W = graphics.getWidth();
        int H = graphics.getHeight();
        screenBuffers = new RenderBuffer(2, W, H, true);
        blurBuffers = new RenderBuffer(2, BUFFER_WIDTH, BUFFER_HEIGHT, false);
        screenRegion = makeRenderRoi(screenBuffers.getTexture(0), W, H, false);
        blurRegion = makeRenderRoi(blurBuffers.getTexture(0), W, H, false);
        addModel();
        addEffect();
        addEnvironment();
    }

    private RenderRoi makeRenderRoi(Texture texture, float width, float height, boolean flipY) {
        float tw = texture.getWidth();
        float th = texture.getHeight();
        float r = Math.max(width / tw, height / th);
        int w = Math.round(tw * r);
        int h = Math.round(th * r);
        return new RenderRoi.Builder()
                .scale(r)
                .setSize(w, h)
                .flip(false, flipY)
                .setRoi((w - width) / 2, (h - height) / 2, width, height)
                .create();
    }

    private void addEffect() {
        particleSystem = new ParticleSystem();

        PointSpriteParticleBatch pointSpriteBatch = new PointSpriteParticleBatch();
        pointSpriteBatch.setCamera(camera);
        particleSystem.add(pointSpriteBatch);
        BillboardParticleBatch bill = new BillboardParticleBatch();
        bill.setCamera(camera);
        particleSystem.add(bill);
        ModelInstanceParticleBatch buf = new ModelInstanceParticleBatch();
        particleSystem.add(buf);

        ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());
        ParticleEffectLoader loader = new ParticleEffectLoader(new InternalFileHandleResolver());
        assets.setLoader(ParticleEffect.class, loader);
        assets.load(EFFECT, ParticleEffect.class, loadParam);
        assets.finishLoading();

        effect = assets.get(EFFECT, ParticleEffect.class).copy();
        effect.translate(new Vector3(-0.006f, 7.27380f, 4.0610f));
        effect.rotate(Vector3.Y, 0);
        effect.rotate(Vector3.X, 125);
        effect.init();
        effect.start();
        particleSystem.add(effect);
    }

    private void addModel() {
        assets.load(MODEL, Model.class);
        assets.load(MODEL_ADD, Model.class);
        assets.load(Texture_D, Texture.class);
        assets.load(Texture_N, Texture.class);
        assets.load(Texture_E, Texture.class);
        assets.load(Texture_S, Texture.class);
        assets.finishLoading();

        Model model = assets.get(MODEL, Model.class);
        Material material = model.materials.get(0);
        material.clear();
        material.set(TextureAttribute.createDiffuse(assets.get(Texture_D, Texture.class)));
        material.set(TextureAttribute.createNormal(assets.get(Texture_N, Texture.class)));
        material.set(TextureAttribute.createEmissive(assets.get(Texture_E, Texture.class)));
        material.set(TextureAttribute.createSpecular(assets.get(Texture_S, Texture.class)));
        material.set(new ColorAttribute(ColorAttribute.Emissive, 2, 2, 2, 2));
        model.materials.add(material);
        instances.add(new ModelInstance(model));
        instances.add(new ModelInstance(assets.get(MODEL_ADD, Model.class)));
    }

    private void addEnvironment() {
        PointLightsAttribute attr = new PointLightsAttribute();
        attr.lights.add(new PointLight().set(Color.GREEN, 0, 0, -3, 15f));

        ModelBuilder builder = new ModelBuilder();
        Material material = new Material();
        material.set(ColorAttribute.createDiffuse(Color.DARK_GRAY));

        for (int i = 0; i < 10; i++) {
            float w = (float) (1 + 2f * Math.random());
            float h = (float) (1 + 6f * Math.random());
            float d = (float) (1 + 2f * Math.random());
            float x = (float) (Math.random() - 0.5);
            x = 3 * x / Math.abs(x) + 10 * x;
            float y = h / 2;
            float z = (float) (Math.random() - 0.5);
            z = 3 * z / Math.abs(z) + 10 * z;
            Model m1 = builder.createBox(w, h, w, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
            ModelInstance instance = new ModelInstance(m1);
            Vector3 pos = new Vector3(x, y, z);
            instance.transform.translate(pos);
            instances.add(instance);
        }

        material.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.GRAY));
        Model m2 = builder.createLineGrid(30, 30, 1f, 1f, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        ModelInstance i2 = new ModelInstance(m2);
        i2.transform.translate(0, 0, 0);
        instances.add(i2);
    }

    private void renderModel(int size, boolean particle) {
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera);
        if (particleSystem != null && particle) {
            particleSystem.update();
            particleSystem.begin();
            particleSystem.draw();
            particleSystem.end();
            modelBatch.render(particleSystem);
        }
        renderables.clear();
        renderables.addAll(instances, 0, size);
        if (particle) modelBatch.render(renderables, environment);
        else modelBatch.render(renderables);
        modelBatch.end();
    }

    int screenWidth, screenHeight;
    int[] blurRoi;

    @Override
    public void render() {
        if (instances.size <= 0) return;
        //renderModel();
        //HDR
        screenWidth = screenBuffers.getTexture(0).getWidth();
        screenHeight = screenBuffers.getTexture(0).getHeight();
        screenBuffers.get(0).begin();
        gl.glViewport(0, 0, screenWidth, screenHeight);
        gl.glClearColor(0, 0, 0, 0);
        renderModel(instances.size, true);
        screenBuffers.get(0).end();
        //render glow models
        screenBuffers.get(1).begin();
        gl.glViewport(0, 0, screenWidth, screenHeight);
        gl.glClearColor(0, 0, 0, 0);
        renderModel(1, false);
        screenBuffers.get(1).end();
        //Sample
        blurBuffers.get(0).begin();
        gl.glViewport(0, 0, blurBuffers.get(1).getWidth(), blurBuffers.get(1).getHeight());
        gl.glClearColor(0, 0, 0, 1f);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        mesh.setProjection(0, 0, blurBuffers.get(1).getWidth(), blurBuffers.get(1).getHeight());
        mesh.setShader(samplerShader);
        mesh.begin();
        samplerShader.setUniformf("enhance", enhance * (float) (Math.cos(time) + 1f));
        mesh.setRenderRoi(0, screenRegion);
        mesh.bindTexture(0, screenBuffers.getTexture(0));
        mesh.setRenderRoi(1, screenRegion);
        mesh.bindTexture(1, screenBuffers.getTexture(1));
        blurRegion.save();
        blurRegion.scale(1, 1);
        blurRoi = blurRegion.getRoi();
        blurRegion.restore();
        mesh.render(blurRoi);
        mesh.end();
        blurBuffers.get(0).end();
        //Blur
        hBlur(blurBuffers.get(0), blurBuffers.get(1), 1);
        vBlur(blurBuffers.get(1), blurBuffers.get(0), 1);
        hBlur(blurBuffers.get(0), blurBuffers.get(1), 1);
        vBlur(blurBuffers.get(1), blurBuffers.get(0), 1);
        //Render
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        gl.glViewport(0, 0, graphics.getWidth(), graphics.getHeight());
        mesh.setShader(renderShader);
        mesh.begin();
        mesh.setProjection(0, 0, graphics.getWidth(), graphics.getHeight());
        renderShader.setUniformf("exposure", exposure);
        mesh.setRenderRoi(0, screenRegion);
        mesh.bindTexture(0, screenBuffers.getTexture(0));
        mesh.setRenderRoi(1, blurRegion);
        mesh.bindTexture(1, blurBuffers.getTexture(0));
        mesh.render(0, 0, graphics.getWidth(), graphics.getHeight());
        mesh.end();

        if (input.isKeyPressed(Input.Keys.UP)) {
            exposure += .1f;
        } else if (input.isKeyPressed(Input.Keys.DOWN)) {
            exposure -= .1f;
        } else if (input.isKeyPressed(Input.Keys.LEFT) || input.isKeyPressed(Input.Keys.VOLUME_UP)) {
            enhance -= .1f;
        } else if (input.isKeyPressed(Input.Keys.RIGHT) || input.isKeyPressed(Input.Keys.VOLUME_DOWN)) {
            enhance += .1f;
        }

        if (time > 2 * Math.PI) {
            time = 0;
        } else {
            time += 0.05f;
        }
        app.log(TAG, graphics.getFramesPerSecond() + "FPS");
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        //screenRegion = makeRenderRoi(screenBuffers.getTexture(0), width, height, false);
        blurRegion = makeRenderRoi(blurBuffers.getTexture(0), width, height, false);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        assets.dispose();
        mesh.dispose();
        screenBuffers.dispose();
        samplerShader.dispose();
        blurShader.dispose();
        renderShader.dispose();
    }

    void hBlur(FrameBuffer in, FrameBuffer out, int step) {
        blur(in, out, 1, 0, step);
    }

    void vBlur(FrameBuffer in, FrameBuffer out, int step) {
        blur(in, out, 0, 1, step);
    }

    void blur(FrameBuffer in, FrameBuffer out, float x, float y, int step) {
        out.begin();
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mesh.setShader(blurShader);
        mesh.setProjection(0, 0, out.getWidth(), out.getHeight());
        mesh.begin();
        blurShader.setUniformf("size", in.getWidth(), in.getHeight());
        blurShader.setUniformf("dir", x, y);
        blurShader.setUniformf("sampleStep", step);
        mesh.setRenderRoi(0, null);
        mesh.bindTexture(0, in.getColorBufferTexture());
        mesh.render(0, 0, out.getWidth(), out.getHeight());
        mesh.end();
        out.end();
    }

    class RenderBuffer implements Disposable {
        final int count;
        int index = 0;
        FrameBuffer[] buffers;

        public RenderBuffer(final int count, int w, int h, boolean depth) {
            this.count = count;
            buffers = new FrameBuffer[count];
            FrameBufferBuilder builder = new FrameBufferBuilder(w, h);
            builder.addColorTextureAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT);
            if (depth) builder.addBasicDepthRenderBuffer();
            for (int i = 0; i < count; i++) {
                buffers[i] = builder.build();
                buffers[i].getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        }

        public FrameBuffer get(int index) {
            return buffers[index];
        }

        public Texture getTexture(int index) {
            return get(index).getColorBufferTexture();
        }

        public FrameBuffer current() {
            return get(index);
        }

        public Texture currentTexture() {
            return current().getColorBufferTexture();
        }

        public FrameBuffer next() {
            return get((index + 1) % count);
        }

        public Texture nextTexture() {
            return next().getColorBufferTexture();
        }

        public FrameBuffer prev() {
            return get((index + count - 1) % count);
        }

        public Texture prevTexture() {
            return prev().getColorBufferTexture();
        }

        public int step() {
            return index = (index + 1) % count;
        }

        @Override
        public void dispose() {
            for (FrameBuffer buffer : buffers)
                buffer.dispose();
        }
    }

    class CartoonShaderProvider extends DefaultShaderProvider {
        DefaultShader.Config config;
        DefaultShader defaultShader;

        public CartoonShaderProvider(DefaultShader.Config defaultConfig) {
            super(defaultConfig);
            config = new DefaultShader.Config();
            config.vertexShader = Gdx.files.internal("shader/cartoon.vertex.glsl").readString();
            config.fragmentShader = Gdx.files.internal("shader/cartoon.fragment.glsl").readString();
        }

        @Override
        protected Shader createShader(Renderable renderable) {
            defaultShader = new DefaultShader(renderable, config);
            return defaultShader;
        }

    }
}
