# Pinned to the builder's native arch: the Vite output is plain static JS/CSS/HTML,
# architecture-independent -- running npm/esbuild under QEMU for arm64 buys nothing
# and esbuild in particular is prone to hanging under QEMU's syscall emulation.
FROM --platform=$BUILDPLATFORM node:22-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM caddy:2-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /workspace/dist /srv
