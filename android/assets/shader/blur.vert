#version 300 es

layout(location = 0) in vec4 a_position;
layout(location = 1) in vec2 a_texCoord0;
layout(location = 2) in vec4 a_color;
out vec4 v_color;
out vec2 v_texCoord;
uniform mat4 u_projTrans;

void main(){
    gl_Position = u_projTrans * a_position;
    v_texCoord = a_texCoord0;
    v_color = a_color;
}