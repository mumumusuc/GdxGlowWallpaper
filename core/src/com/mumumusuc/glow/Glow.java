package com.mumumusuc.glow;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
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

public class Glow extends ApplicationAdapter {
    static final String TAG = Glow.class.getSimpleName();
    static final String SHADER_VERT = "shader/blur.vert";
    static final String SHADER_FRAG = "shader/blur.frag";
    static final String RENDER_FRAG = "shader/render.frag";
    static final String SAMPLER_FRAG = "shader/sampler.frag";
    static final String MODEL = "models/godzilla.g3db";
    static final String EFFECT = "effects/beam.pfx";
    static final String ELEC = "effects/elec.pfx";

    PerspectiveCamera camera;
    CameraInputController controller;
    AssetManager assets;
    Environment environment;
    ModelBatch modelBatch;
    Array<ModelInstance> instances = new Array<ModelInstance>();
    PointLight light;
    ParticleEffect effect;
    ParticleSystem particleSystem;
    /**/
    ShaderProgram samplerShader, blurShader, renderShader;
    TextureRenderer mesh;
    RenderRoi screenRegion, bufferRegion;
    /**
     * render contents to this buffer(2 texture), use MRT is supported
     */
    RenderBuffer screenBuffers;
    float time = 0;
    int BUFFER_WIDTH, BUFFER_HEIGHT;

    @Override
    public void create() {
        samplerShader = new ShaderProgram(files.internal(SHADER_VERT), files.internal(SAMPLER_FRAG));
        if (!samplerShader.isCompiled()) {
            throw new IllegalArgumentException(samplerShader.getLog());
        }
        blurShader = new ShaderProgram(files.internal(SHADER_VERT), files.internal(SHADER_FRAG));
        if (!blurShader.isCompiled()) {
            throw new IllegalArgumentException(blurShader.getLog());
        }
        renderShader = new ShaderProgram(files.internal(SHADER_VERT), files.internal(RENDER_FRAG));
        if (!renderShader.isCompiled()) {
            throw new IllegalArgumentException(renderShader.getLog());
        }
        /**/
        camera = new PerspectiveCamera(67, graphics.getWidth(), graphics.getHeight());
        camera.position.set(0, 0, 3);
        camera.lookAt(0, 0, 0);
        camera.far = 1000;
        camera.near = 1;
        camera.update();
        controller = new CameraInputController(camera);
        Gdx.input.setInputProcessor(controller);
        /**/
        environment = new Environment();
        float rgb = .0f;
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, rgb, rgb, rgb, 1f));
        //Vector3 pos = new Vector3(-.1f, .9f, -.6f);
        Vector3 pos = new Vector3(-.0f, 0.9f, .8f);
        light = new PointLight().set(Color.PINK, new Vector3(-.1f, 6f, -3f), 10.0f);
        //environment.add(light);
        environment.add(new DirectionalLight().set(Color.WHITE, 0, 0, -1));
        /**/
        assets = new AssetManager();
        assets.load(MODEL, Model.class);
        //assets.load(EFFECT, ParticleEffect.class);
        assets.update();
        assets.finishLoading();
        addModel();
        //  addEffect();
        addEnvironment();
        /**/
        modelBatch = new ModelBatch(new CartoonShaderProvider(null));
        /**/
        mesh = new TextureRenderer();
        screenRegion = new RenderRoi.Builder().setSize(graphics.getWidth(), graphics.getHeight()).flip(false, false).create();
        BUFFER_WIDTH = graphics.getWidth();
        BUFFER_HEIGHT = graphics.getHeight();
        screenBuffers = new RenderBuffer(3, BUFFER_WIDTH, BUFFER_HEIGHT);
        bufferRegion = new RenderRoi.Builder().setSize(BUFFER_WIDTH, BUFFER_HEIGHT).flip(false, false).create();
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
        assets.load(ELEC, ParticleEffect.class, loadParam);
        assets.update();
        assets.finishLoading();

        effect = assets.get(EFFECT, ParticleEffect.class).copy();
        Matrix4 mat = new Matrix4();
        mat.translate(0, 0, 1);
        //mat.scale(.1f, .1f, .1f);
        //effect.setTransform(mat);
        effect.rotate(Vector3.Y, 20);
        effect.rotate(Vector3.X, 125);
        //effect.translate(new Vector3(0.05f, 0.7f, 1.f));
        effect.translate(new Vector3(-0.28f, 1.2f, -1.15f));

        //effect.scale(.2f, .2f, .2f);
        effect.init();
        effect.start();  // optional: particle will begin playing immediately

        ParticleEffect e = assets.get(ELEC, ParticleEffect.class).copy();
        e.translate(new Vector3(-.1f, 0.46f, -.09f));
        e.rotate(Vector3.X, -58);
        e.init();
        e.start();

        particleSystem.add(effect);
        particleSystem.add(e);

    }

    private void addModel() {
        Model model = assets.get(MODEL, Model.class);
        Texture Z = new Texture(files.internal("models/Godzilla_Z.tga"));
        Texture D = new Texture(files.internal("models/Godzilla_D.tga"));
        Texture E = new Texture(files.internal("models/Godzilla_E.tga"));
        Texture N = new Texture(files.internal("models/Godzilla_N.tga"));
        Texture S = new Texture(files.internal("models/Godzilla_S.tga"));

        Material material = model.materials.get(0);
        material.clear();
        //material.set(ColorAttribute.createDiffuse(Color.WHITE));
        material.set(TextureAttribute.createDiffuse(D));
        material.set(TextureAttribute.createNormal(N));
        material.set(TextureAttribute.createEmissive(E));
        //material.set(TextureAttribute.createSpecular(S));
        model.materials.add(material);
        instances.add(new ModelInstance(model));
    }

    private void addEnvironment() {
        PointLightsAttribute attr = new PointLightsAttribute();
        attr.lights.add(new PointLight().set(Color.GREEN, 0, 0, -3, 15f));

        DirectionalLightsAttribute dirc = new DirectionalLightsAttribute();
        dirc.lights.add(new DirectionalLight().set(Color.GOLD, 1, 0, 0));

        ModelBuilder builder = new ModelBuilder();
        Material material = new Material();
        material.set(ColorAttribute.createDiffuse(Color.WHITE));
        // material.set(new BlendingAttribute(.5f));
        //material.set(attr);
        //material.set(dirc);
        Model model = builder.createBox(.2f, .2f, .2f, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        ModelInstance instance = new ModelInstance(model);
//        instance.transform.translate(light.position);
        //instances.add(instance);

/*
        material.set(ColorAttribute.createDiffuse(Color.WHITE));
        Model m2 = builder.createLineGrid(60, 60, .1f, .1f, material, VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates);
        ModelInstance i2 = new ModelInstance(m2);
        i2.transform.translate(0, -.9f, 0);
        instances.add(i2);
*/
    }

    private void renderModel() {
        gl.glViewport(0, 0, graphics.getWidth(), graphics.getHeight());
        gl.glClearColor(.5f, .5f, .5f, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        //gl.glEnable(GL20.GL_BLEND);
        //gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        //gl.glDepthMask(false);

        modelBatch.begin(camera);
        if (particleSystem != null) {
            particleSystem.update(); // technically not necessary for rendering
            particleSystem.begin();
            particleSystem.draw();
            particleSystem.end();
            modelBatch.render(particleSystem);
        }
        modelBatch.getRenderContext().setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        modelBatch.render(instances, environment);
        modelBatch.end();
    }


    @Override
    public void render() {
        //    screenBuffers.get(0).begin();
        renderModel();
    /*    screenBuffers.get(0).end();

        screenBuffers.get(1).begin();
        mesh.setShader(samplerShader);
        mesh.begin();
        gl.glClearColor(0, 0, 0, 1f);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        //mesh.setColor(r, g, b, 1f);
        mesh.setProjection(0, 0, BUFFER_WIDTH, BUFFER_HEIGHT);
        mesh.render(bufferRegion, screenBuffers.getTexture(0), 0, 0);
        mesh.end();
        screenBuffers.get(1).end();

        hBlur(screenBuffers.get(1), screenBuffers.get(2), 1);
        vBlur(screenBuffers.get(2), screenBuffers.get(1), 1);
        for (int i = 0; i < 8; i++) {
            hBlur(screenBuffers.get(1), screenBuffers.get(2), 5);
            vBlur(screenBuffers.get(2), screenBuffers.get(1), 5);
        }
        hBlur(screenBuffers.get(1), screenBuffers.get(2), 1);
        vBlur(screenBuffers.get(2), screenBuffers.get(1), 1);

        gl.glClearColor(1, 1, 1, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        gl.glViewport(0, 0, graphics.getWidth(), graphics.getHeight());
        mesh.setShader(renderShader);
        mesh.begin();
        mesh.setProjection(0, 0, graphics.getWidth(), graphics.getHeight());
        int handle = screenBuffers.getTexture(1).getTextureObjectHandle();
        screenBuffers.getTexture(1).bind(handle);
        renderShader.setUniformi("texture_1", handle);
        screenBuffers.getTexture(1).setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        mesh.render(bufferRegion, screenBuffers.getTexture(0), 0, 0);
        mesh.end();
*/
        //       app.log(TAG, graphics.getFramesPerSecond() + "FPS");
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
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
        mesh.setShader(blurShader);
        mesh.setProjection(0, 0, in.getWidth(), in.getHeight());
        out.begin();
        mesh.begin();
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        blurShader.setUniformf("size", in.getWidth(), in.getHeight());
        blurShader.setUniformf("dir", x, y);
        blurShader.setUniformf("sampleStep", step);
        mesh.render(bufferRegion, in.getColorBufferTexture(), 0, 0, in.getWidth(), in.getHeight());
        mesh.end();
        out.end();
    }

    class RenderBuffer implements Disposable {
        final int count;
        int index = 0;
        FrameBuffer[] buffers;

        public RenderBuffer(final int count, int w, int h) {
            this.count = count;
            buffers = new FrameBuffer[count];
            FrameBufferBuilder builder = new FrameBufferBuilder(w, h);
            builder.addColorTextureAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT);
            builder.addBasicDepthRenderBuffer();
            for (int i = 0; i < count; i++) {
                buffers[i] = builder.build();
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

        public CartoonShaderProvider(DefaultShader.Config defaultConfig) {
            super(defaultConfig);
            config = new DefaultShader.Config();
            config.vertexShader = Gdx.files.internal("shader/cartoon.vertex.glsl").readString();
            config.fragmentShader = Gdx.files.internal("shader/cartoon.fragment.glsl").readString();
        }

        @Override
        protected Shader createShader(Renderable renderable) {
            return new DefaultShader(renderable, config);
        }
    }
}
