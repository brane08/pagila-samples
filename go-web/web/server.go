package web

import (
	"log"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func StartServer(port int) {
	log.Printf("Starting server on port %d", port)
	r := gin.Default()
	r.GET("/ping", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"message": "pong",
		})
	})
	r.Run(":" + strconv.Itoa(port))
}
