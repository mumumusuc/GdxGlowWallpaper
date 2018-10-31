#version 300 es

layout(location = 0) in vec4 a_position;
layout(location = 1) in vec2 a_texCoord0;
layout(location = 2) in vec2 a_texCoord1;
out vec2 v_texCoord0;
out vec2 v_texCoord1;
uniform mat4 u_projTrans;

void main(){
    gl_Position = u_projTrans * a_position;
    v_texCoord0 = a_texCoord0;
    v_texCoord1 = a_texCoord1;
}