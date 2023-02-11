FROM milekat/minecraft-dev:1.19.2

#   Add additonal plugins
RUN mkdir -p /home/minecraft/plugins
COPY dev/plugins/* /home/minecraft/plugins

#   Add builded plugin
COPY build/libs/*.jar /home/minecraft/plugins
#   Add config.yml for this plugin
RUN mkdir -p /home/minecraft/plugins/CustomShops
COPY dev/configs/config.yml /home/minecraft/plugins/CustomShops