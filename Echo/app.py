from src.echo.editor import build_app


if __name__ == "__main__":
    app = build_app()
    app.launch(server_name="127.0.0.1", inbrowser=False)
