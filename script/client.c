#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <ctype.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <errno.h>
int main(int argc,char *argv[])
{
    int port=atoi(argv[2]);
    int st=atoi(argv[3]);
    int server_fd;
    struct sockaddr_in servaddr;
    bzero(&servaddr,sizeof(servaddr));
    servaddr.sin_family=AF_INET;
    servaddr.sin_port=htons(port);
    inet_pton(AF_INET,argv[1],(void*)&servaddr.sin_addr);
    server_fd=socket(AF_INET,SOCK_STREAM,0);
    int ret=connect(server_fd,(const struct sockaddr *)&servaddr,
                    sizeof(struct sockaddr));
     if(ret<0)
     {
      perror("error: socket connect!");
      exit(1);
     }
     sleep(st);
     close(server_fd);
    //do do do
    return 1;
}
