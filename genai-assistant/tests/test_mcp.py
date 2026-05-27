# test_mcp.py
import logging

from fastmcp import FastMCP

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s - %(message)s',
                    handlers=[logging.StreamHandler()])

mcp = FastMCP("Test")


@mcp.tool()
def ping():
    return "pong"


print("Using FastMCP from:", mcp.__class__.__module__)

if __name__ == "__main__":
    mcp.run("streamable-http", host="0.0.0.0", port=8001)
