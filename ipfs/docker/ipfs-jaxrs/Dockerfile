FROM nessusio/fedoraj:29

# Install the binaries
COPY /maven/nessus-ipfs-dist-${project.version} nessus-ipfs-jaxrs

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-jaxrs/bin/run-nessus-jaxrs.sh /usr/local/bin/run-nessus-jaxrs

# Expose the JAXRS port
EXPOSE 8081

ENTRYPOINT ["run-nessus-jaxrs"]