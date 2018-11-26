FROM nessusio/fedoraj:29

# Install the binaries
COPY /maven/nessus-ipfs-dist-${project.version} nessus-ipfs-webui

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-webui/bin/run-nessus-webui.sh /usr/local/bin/nessus-webui

ENTRYPOINT ["nessus-webui"]