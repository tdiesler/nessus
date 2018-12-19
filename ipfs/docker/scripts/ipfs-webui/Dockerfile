FROM nessusio/fedoraj:29

# Install the binaries
COPY /maven/nessus-ipfs-dist-${project.version} nessus-ipfs-webui

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-webui/bin/run-webui.sh /usr/local/bin/run-webui

ENTRYPOINT ["run-webui"]