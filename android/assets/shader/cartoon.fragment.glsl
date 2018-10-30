#ifdef GL_ES
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#if defined(colorFlag)
varying vec4 v_color;
#endif

#ifdef blendedFlag
varying float v_opacity;
#ifdef alphaTestFlag
varying float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#ifdef diffuseTextureFlag
varying MED vec2 v_diffuseUV;
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularTextureFlag
varying MED vec2 v_specularUV;
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
varying MED vec2 v_normalUV;
uniform sampler2D u_normalTexture;
#endif

#ifdef emissiveTextureFlag
varying MED vec2 v_emissiveUV;
uniform sampler2D u_emissiveTexture;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef normalFlag
    varying vec3 v_normal;
#endif

#ifdef cameraPositionFlag
    varying v_viewDir;
#endif

#ifdef shininessFlag
    uniform float u_shininess;
#else
    const float u_shininess = 20.0;
#endif // shininessFlag

#ifdef lightingFlag
    varying vec3 v_lightDiffuse;
    varying vec3 v_lightSpecular;
    #if	defined(ambientLightFlag)
        #define ambientFlag
    #endif
    #if numDirectionalLights > 0
        struct DirectionalLight{
            vec3 color;
            vec3 direction;
        };
        varying DirectionalLight v_dirLights[numDirectionalLights];
    #endif
    #if numPointLights > 0
        struct PointLight{
                vec3 color;
                vec3 position;
                vec3 direction;
        };
        varying PointLight v_pointLights[numPointLights];
    #endif
#endif

vec3 calDirectLightDiffuse(vec3 normal, vec3 lightDir, vec3 lightColor);
vec3 calDirectLightSpecular(vec3 normal, vec3 viewDir, vec3 lightDir, vec3 lightColor,float shininess);
vec3 calPointLightDiffuse(vec3 normal, vec3 lightPosition, vec3 lightColor);
vec3 calPointLightSpecular(vec3 normal, vec3 viewDir, vec3 lightDir, vec3 lightColor, float shininess);

void main() {
	#if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor * v_color;
	#elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor;
	#elif defined(diffuseTextureFlag) && defined(colorFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * v_color;
	#elif defined(diffuseTextureFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV);
	#elif defined(diffuseColorFlag) && defined(colorFlag)
		vec4 diffuse = u_diffuseColor * v_color;
	#elif defined(diffuseColorFlag)
		vec4 diffuse = u_diffuseColor;
	#elif defined(colorFlag)
		vec4 diffuse = v_color;
	#else
		vec4 diffuse = vec4(1.0);
	#endif

	#ifdef blendedFlag
		gl_FragColor.a = diffuse.a * v_opacity;
		#ifdef alphaTestFlag
			if (gl_FragColor.a <= v_alphaTest){
				//discard;
				gl_FragColor.a = 1.0;
			}
		#endif
	#else
		gl_FragColor.a = 1.0;
	#endif
/***************************normal texture*************************/
    #if defined(normalTextureFlag)
        vec3 normal = texture2D(u_normalTexture, v_normalUV).rgb;
        normal = normalize(normal * 2.0 - 1.0);
    #elif defined(normalFlag)
        vec3 normal = v_normal;
    #endif
/***************************lights*************************/
    #if defined(lightingFlag)
        #if (numDirectionalLights > 0) && defined(normalFlag)
            for (int i = 0; i < numDirectionalLights; i++) {
                v_lightDiffuse += calDirectLightDiffuse(normal, v_dirLights[i].direction, v_dirLights[i].color);
                #ifdef specularFlag
                    v_lightSpecular += calDirectLightSpecular(normal, v_viewDir,v_dirLights[i].direction, v_dirLights[i].color, u_shininess);
                #endif
            }
        #endif
        #if (numPointLights > 0) && defined(normalFlag)
            for (int i = 0; i < numPointLights; i++) {
                v_lightDiffuse += calPointLightDiffuse(normal, v_pointLights[i].direction, v_pointLights[i].color);
                #ifdef specularFlag
                    v_lightSpecular += calPointLightSpecular(normal, v_viewDir, v_pointLights[i].direction, v_pointLights[i].color, u_shininess);
                #endif
            }
        #endif
        //TODO: calculate spot-lights
    #endif
/***************************diffuse texture******************************/
    #if (!defined(lightingFlag))
		gl_FragColor.rgb = diffuse.rgb;
	#elif (!defined(specularFlag))
        gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse);
	#else
		#if defined(specularTextureFlag) && defined(specularColorFlag)
			vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * u_specularColor.rgb * v_lightSpecular;
		#elif defined(specularTextureFlag)
			vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * v_lightSpecular;
		#elif defined(specularColorFlag)
			vec3 specular = u_specularColor.rgb * v_lightSpecular;
		#else
			vec3 specular = v_lightSpecular;
		#endif
        gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse) + specular;
	#endif //lightingFlag
/***************************specular texture*****************************/
/***************************emissive texture*****************************/
    #if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
        vec3 emissive = texture2D(u_emissiveTexture, v_emissiveUV).rgb * u_emissiveColor.rgb;
    #elif defined(emissiveTextureFlag)
        vec3 emissive = texture2D(u_emissiveTexture, v_emissiveUV).rgb ;
    #elif defined(emissiveColorFlag)
        vec3 emissive = u_emissiveColor.rgb;
    #else
        vec3 emissive = vec3(1.0);
    #endif
/****************************final******************************/
    gl_FragColor.rgb += emissive;
}

vec3 calDirectLightDiffuse(vec3 normal, vec3 lightDir, vec3 lightColor){
    vec3 dir = -lightDir;
	float NdotL = clamp(dot(normal, dir), 0.0, 1.0);
	return lightColor * NdotL;
}

vec3 calDirectLightSpecular(vec3 normal, vec3 viewDir, vec3 lightDir, vec3 lightColor, float shininess){
    float halfDotView = max(0.0, dot(normal, normalize(lightDir + viewDir)));
    return  lightColor * pow(halfDotView, shininess);
}

vec3 calPointLightDiffuse(vec3 normal, vec3 lightDir, vec3 lightColor){
    float dist2 = dot(lightDir, lightDir);
    lightDir *= inversesqrt(dist2);
    float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
    return lightColor * (NdotL / (1.0 + dist2));
}
vec3 calPointLightSpecular(vec3 normal, vec3 viewDir, vec3 lightDir, vec3 lightColor, float shininess){
    float halfDotView = max(0.0, dot(normal, normalize(lightDir + viewDir)));
    return lightColor * pow(halfDotView, shininess);
}