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
uniform vec2 dir;
uniform vec2 size;
uniform float time;
uniform float sampleStep;
const int radius = 4;
const float weight[5] = float[5](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main(){
    vec4 sum = vec4(0.0);
    float sub = step(0.,sampleStep)*sampleStep;
    vec2 s = dir / size;
    for(int i = -radius ; i <= radius ; i++){
        sum += texture(texture_0, v_texCoord0 + float(i)*sub*s) * weight[abs(i)];
    }
    gl_FragColor = vec4(sum.rgb, 1.0);
}
