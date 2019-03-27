FROM nessusio/centosj:7

# Install the binaries
COPY /maven/nessus-ipfs-dist-${project.version} nessus-ipfs-jaxrs

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-jaxrs/bin/run-jaxrs.sh /usr/local/bin/run-jaxrs

# Expose the JAXRS port
EXPOSE 8081

ENTRYPOINT ["run-jaxrs"]