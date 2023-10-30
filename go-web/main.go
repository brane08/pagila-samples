package main

import (
	"flag"
	"go-web/web"
)

func main() {
	httpServer := flag.Bool("http", false, "Start http server")
	httpPort := flag.Int("port", 7089, "Start server port")
	flag.Parse()
	if *httpServer {
		web.StartServer(*httpPort)
	}
}
