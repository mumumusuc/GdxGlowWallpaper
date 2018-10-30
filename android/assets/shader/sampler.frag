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
const float gamma = 2.2;

void main(){
    vec3 hdrColor = texture2D(texture_0, v_texCoord).rgb;
    hdrColor = pow(hdrColor, vec3(1.0 / gamma));
    float brightness = dot(hdrColor, vec3(0.2126, 0.7152, 0.0722));
    gl_FragColor = vec4(step(.5, brightness) * hdrColor.rgb , 1.0f);
}
