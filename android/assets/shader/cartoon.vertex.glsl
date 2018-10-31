#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
    #define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
    #define specularFlag
#endif

#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
    #define emissiveFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
    #define cameraPositionFlag
#endif

#if defined(tangentFlag) || defined(binormalFlag)
    #define TBNFlag
#endif

attribute vec3 a_position;
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

#if defined(colorFlag)
    varying vec4 v_color;
    attribute vec4 a_color;
#endif // colorFlag

#ifdef normalFlag
    attribute vec3 a_normal;
    uniform mat3 u_normalMatrix;
    varying vec3 v_normal;
#endif // normalFlag

#ifdef textureFlag
    attribute vec2 a_texCoord0;
#endif // textureFlag

#ifdef diffuseTextureFlag
    uniform vec4 u_diffuseUVTransform;
    varying vec2 v_diffuseUV;
#endif

#ifdef specularTextureFlag
    uniform vec4 u_specularUVTransform;
    varying vec2 v_specularUV;
#endif

#ifdef emissiveTextureFlag
    uniform vec4 u_emissiveUVTransform;
    varying vec2 v_emissiveUV;
#endif

#ifdef tangentFlag
    attribute vec3 a_tangent;
#endif

#ifdef binormalFlag
    attribute vec3 a_binormal;
#endif

#ifdef normalTextureFlag
    uniform vec4 u_normalUVTransform;
    varying vec2 v_normalUV;
#endif

#ifdef blendedFlag
    uniform float u_opacity;
    varying float v_opacity;
    #ifdef alphaTestFlag
        uniform float u_alphaTest;
        varying float v_alphaTest;
    #endif //alphaTestFlag
#endif // blendedFlag

#ifdef lightingFlag
    varying vec3 v_lightDiffuse;
    varying vec3 v_lightSpecular;
    #ifdef ambientLightFlag
        uniform vec3 u_ambientLight;
    #endif
    #ifdef ambientCubemapFlag
        uniform vec3 u_ambientCubemap[6];
    #endif // ambientCubemapFlag
    #ifdef sphericalHarmonicsFlag
        uniform vec3 u_sphericalHarmonics[9];
    #endif //sphericalHarmonicsFlag
    #ifdef specularFlag
        varying vec3 v_viewDir;
    #endif
    #ifdef cameraPositionFlag
        uniform vec4 u_cameraPosition;
    #endif // cameraPositionFlag
    #ifdef fogFlag
        varying float v_fog;
    #endif // fogFlag
    #if numDirectionalLights > 0
        struct DirectionalLight{
            vec3 color;
            vec3 direction;
        };
        uniform DirectionalLight u_dirLights[numDirectionalLights];
        varying vec3 v_dirLightColors[numDirectionalLights];
        varying vec3 v_dirLightDirs[numDirectionalLights];
    #endif // numDirectionalLights
    #if numPointLights > 0
        struct PointLight{
            vec3 color;
            vec3 position;
            vec3 direction;
        };
        uniform PointLight u_pointLights[numPointLights];
        varying vec3 v_pointLightColors[numPointLights];
        varying vec3 v_pointLightDirs[numPointLights];
    #endif // numPointLights
    #if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
        #define ambientFlag
    #endif
    #if defined(ambientFlag) && defined(separateAmbientFlag)
        varying vec3 v_ambientLight;
    #endif
#endif // lightingFlag

void main() {
   	#ifdef diffuseTextureFlag
		v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;
	#endif //diffuseTextureFlag

	#ifdef specularTextureFlag
		v_specularUV = u_specularUVTransform.xy + a_texCoord0 * u_specularUVTransform.zw;
	#endif //specularTextureFlag

	#ifdef normalTextureFlag
        v_normalUV = u_normalUVTransform.xy + a_texCoord0 * u_normalUVTransform.zw;
    #endif //normalTextureFlag

    #ifdef emissiveTextureFlag
        v_emissiveUV = u_emissiveUVTransform.xy + a_texCoord0 * u_emissiveUVTransform.zw;
    #endif //emissiveTextureFlag

	#if defined(colorFlag)
		v_color = a_color;
	#endif // colorFlag

	#ifdef blendedFlag
		v_opacity = u_opacity;
		#ifdef alphaTestFlag
			v_alphaTest = u_alphaTest;
		#endif //alphaTestFlag
	#endif // blendedFlag

	vec4 pos = u_worldTrans * vec4(a_position, 1.0);
	gl_Position = u_projViewTrans * pos;

	#if defined(normalFlag)
		vec3 normal = normalize(u_normalMatrix * a_normal);
		v_normal = normal;
	#endif // normalFlag


    #if defined(TBNFlag)
        vec3 T = normalize(vec3(u_worldTrans * vec4(a_tangent,   0.0)));
        vec3 B = normalize(vec3(u_worldTrans * vec4(a_binormal, 0.0)));
        vec3 N = normalize(vec3(u_worldTrans * vec4(a_normal,    0.0)));
        mat3 TBN = mat3(1.0);
        TBN[0][1] = TBN[1][0];
        TBN[0][2] = TBN[2][0];
        TBN[1][2] = TBN[2][1];
    #else
        mat3 TBN = mat3(vec3(1.,0.,0.),vec3(0.,1.,0.),vec3(0.,0.,1.));
    #endif

	#ifdef fogFlag
        vec3 flen = TBN * (u_cameraPosition.xyz - pos.xyz);
        float distance = dot(flen, flen) * u_cameraPosition.w * 200.0;
        float fog = exp(-pow(distance * 8.0,2.0));
        v_fog = 1.0 - min(fog, 1.0);
    #endif

	#ifdef lightingFlag
		#if	defined(ambientLightFlag)
        	vec3 ambientLight = u_ambientLight;
		#elif defined(ambientFlag)
        	vec3 ambientLight = vec3(0.0);
		#endif
        #ifdef ambientCubemapFlag
			vec3 squaredNormal = normal * normal;
			vec3 isPositive  = step(0.0, normal);
			ambientLight += squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
					squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
					squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
		#endif // ambientCubemapFlag

		#ifdef sphericalHarmonicsFlag
			ambientLight += u_sphericalHarmonics[0];
			ambientLight += u_sphericalHarmonics[1] * normal.x;
			ambientLight += u_sphericalHarmonics[2] * normal.y;
			ambientLight += u_sphericalHarmonics[3] * normal.z;
			ambientLight += u_sphericalHarmonics[4] * (normal.x * normal.z);
			ambientLight += u_sphericalHarmonics[5] * (normal.z * normal.y);
			ambientLight += u_sphericalHarmonics[6] * (normal.y * normal.x);
			ambientLight += u_sphericalHarmonics[7] * (3.0 * normal.z * normal.z - 1.0);
			ambientLight += u_sphericalHarmonics[8] * (normal.x * normal.x - normal.y * normal.y);
		#endif // sphericalHarmonicsFlag

		#ifdef ambientFlag
			#ifdef separateAmbientFlag
				v_ambientLight = ambientLight;
				v_lightDiffuse = vec3(0.0);
			#else
				v_lightDiffuse = ambientLight;
			#endif //separateAmbientFlag
		#else
	        v_lightDiffuse = vec3(0.0);
		#endif //ambientFlag

		#ifdef specularFlag
			v_lightSpecular = vec3(0.0);
			v_viewDir = TBN * normalize(u_cameraPosition.xyz - pos.xyz);
		#endif // specularFlag

		#if (numDirectionalLights > 0) && defined(normalFlag)
			for (int i = 0; i < numDirectionalLights; i++) {
			v_dirLightColors[i] = u_dirLights[i].color;
			v_dirLightDirs[i]= TBN * u_dirLights[i].direction;
		}
		#endif // numDirectionalLights

		#if (numPointLights > 0) && defined(normalFlag)
			for (int i = 0; i < numPointLights; i++) {
			v_pointLightColors[i] = u_pointLights[i].color;
			v_pointLightDirs[i] = TBN * (u_pointLights[i].position - pos.xyz);
		}
		#endif // numPointLights
	#endif // lightingFlag
}