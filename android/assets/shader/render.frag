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
uniform sampler2D texture_1;
const float exposure = 1.;
const float gamma = 1.2;

void main(){
    vec3 hdrColor = texture2D(texture_0, v_texCoord).rgb;
    vec3 bloomColor = texture2D(texture_1, v_texCoord).rgb;
    hdrColor += bloomColor;
    vec3 result = vec3(1.0) - exp(-hdrColor * exposure);
    result = pow(result, vec3(1.0 / gamma));
    gl_FragColor = vec4(result, 1.0f);
}
