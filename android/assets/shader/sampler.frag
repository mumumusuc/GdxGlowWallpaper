#version 300 es
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in vec2 v_texCoord;
in vec4 v_color;
uniform sampler2D texture_0;
uniform float enhance;

void main(){
    vec3 hdrColor = texture2D(texture_0, v_texCoord).rgb;
    float brightness = dot(hdrColor, vec3(0.3, 0.6, 0.1));
    gl_FragColor = vec4(step(0.8, brightness) * hdrColor * enhance, 1.0);
}
