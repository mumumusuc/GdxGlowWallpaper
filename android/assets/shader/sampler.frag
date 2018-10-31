#version 300 es

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in vec2 v_texCoord;
in vec4 v_color;
out vec4 gl_FragColor;
uniform sampler2D texture_0;
uniform float enhance;

void main(){
    vec4 hdrColor = texture(texture_0, v_texCoord);
    float brightness = dot(hdrColor.rgb, vec3(0.3, 0.6, 0.1));
    gl_FragColor = vec4(step(0.8, brightness) * hdrColor.rgb * enhance, hdrColor.a);
}
