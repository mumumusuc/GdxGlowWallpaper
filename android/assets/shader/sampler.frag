#version 300 es

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in vec2 v_texCoord0;
in vec2 v_texCoord1;
out vec4 gl_FragColor;
uniform sampler2D texture_0;
uniform sampler2D texture_1;
uniform float enhance;

void main(){
    vec4 hdrColor = texture(texture_0, v_texCoord0);
    vec4 maskColor = texture(texture_1,v_texCoord1);
    float brightness0 = dot(hdrColor.rgb, vec3(0.3, 0.6, 0.1));
    float brightness1 = dot(maskColor.rgb, vec3(0.3, 0.6, 0.1));
    brightness0 = step(0.8, brightness0);
    brightness1 = step(0.8, brightness1);
    gl_FragColor = vec4(brightness0 * brightness1 * hdrColor.rgb * enhance, hdrColor.a);
}
