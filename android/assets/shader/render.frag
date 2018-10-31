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
uniform float exposure;
const float gamma = 2.2;

void main(){
    vec3 hdrColor = texture(texture_0, v_texCoord0).rgb;
    vec3 bloomColor = texture(texture_1, v_texCoord1).rgb;
    hdrColor += bloomColor;
    vec3 result = vec3(1.0) - exp(-hdrColor * exposure);
    result = pow(result, vec3(1.0 / gamma));
    gl_FragColor = vec4(result, 1.0f);
}
