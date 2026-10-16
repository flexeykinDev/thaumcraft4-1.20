#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Radius;

out vec4 fragColor;

// Размытие вдоль одной оси: ближние точки весят больше дальних. Два прохода (по X и по Y) дают мягкое пятно.
void main() {
    vec3 blurred = vec3(0.0);
    float total = 0.0;
    for (float r = -Radius; r <= Radius; r += 1.0) {
        float strength = 1.0 - abs(r) / (Radius + 1.0);
        blurred += texture(DiffuseSampler, texCoord + oneTexel * r * BlurDir).rgb * strength;
        total += strength;
    }
    fragColor = vec4(blurred / total, 1.0);
}
