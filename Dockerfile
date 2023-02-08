FROM milekat/minecraft-dev:1.19.2
RUN mkdir /home/minecraft/plugins/CustomShops
COPY dev/configs/config.yml /home/minecraft/plugins/CustomShops
COPY dev/libs/*.jar /home/minecraft/plugins
COPY build/libs/*.jar /home/minecraft/plugins