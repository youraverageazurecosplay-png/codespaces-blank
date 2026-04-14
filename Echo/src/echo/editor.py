from __future__ import annotations

import tempfile
from pathlib import Path

import gradio as gr
from PIL import Image

from .pipelines import EchoPipeline, EditSettings


PIPELINE = EchoPipeline()


def _build_settings(
    brightness: float,
    contrast: float,
    saturation: float,
    sharpness: float,
    blur: float,
    resize_percent: int,
    auto_enhance: bool,
    stylize_edges: bool,
    detail_boost: bool,
    portrait_blur: bool,
) -> EditSettings:
    return EditSettings(
        brightness=brightness,
        contrast=contrast,
        saturation=saturation,
        sharpness=sharpness,
        blur=blur,
        resize_percent=resize_percent,
        auto_enhance=auto_enhance,
        stylize_edges=stylize_edges,
        detail_boost=detail_boost,
        portrait_blur=portrait_blur,
    )


def apply_edits(
    image: Image.Image,
    brightness: float,
    contrast: float,
    saturation: float,
    sharpness: float,
    blur: float,
    resize_percent: int,
    auto_enhance: bool,
    stylize_edges: bool,
    detail_boost: bool,
    portrait_blur: bool,
):
    if image is None:
        raise gr.Error("Upload an image to start editing.")

    settings = _build_settings(
        brightness,
        contrast,
        saturation,
        sharpness,
        blur,
        resize_percent,
        auto_enhance,
        stylize_edges,
        detail_boost,
        portrait_blur,
    )
    return PIPELINE.basic_adjust(image, settings)


def preview_mask(image: Image.Image):
    if image is None:
        raise gr.Error("Upload an image first.")
    return PIPELINE.mask_preview(image)


def apply_ai_edit(
    image: Image.Image,
    prompt: str,
    region_x: int,
    region_y: int,
    region_width: int,
    region_height: int,
):
    if image is None:
        raise gr.Error("Upload an image before using AI Assist.")

    try:
        return PIPELINE.prompt_edit(
            image,
            prompt,
            region_x,
            region_y,
            region_width,
            region_height,
        )
    except ValueError as exc:
        raise gr.Error(str(exc)) from exc


def generate_drawing(prompt: str):
    try:
        return PIPELINE.draw_subject(prompt)
    except ValueError as exc:
        raise gr.Error(str(exc)) from exc


def export_image(image: Image.Image):
    if image is None:
        raise gr.Error("Generate an edited image before exporting.")

    export_dir = Path(tempfile.gettempdir()) / "echo_exports"
    export_dir.mkdir(parents=True, exist_ok=True)
    export_path = export_dir / "echo_export.png"
    image.save(export_path, format="PNG")
    return str(export_path)


def reset_controls():
    return (
        1.0,
        1.0,
        1.0,
        1.0,
        0.0,
        100,
        False,
        False,
        False,
        False,
    )


def build_app() -> gr.Blocks:
    theme = gr.themes.Soft(
        primary_hue="slate",
        secondary_hue="rose",
        neutral_hue="zinc",
    )

    with gr.Blocks(theme=theme, title="Echo") as app:
        gr.Markdown(
            """
            # Echo
            Offline AI photo editing with local-first enhancement, masking, and stylization.
            """
        )

        with gr.Row():
            input_image = gr.Image(type="pil", label="Input Image")
            output_image = gr.Image(type="pil", label="Edited Output")

        with gr.Row():
            with gr.Column():
                brightness = gr.Slider(0.2, 2.5, value=1.0, step=0.05, label="Brightness")
                contrast = gr.Slider(0.2, 2.5, value=1.0, step=0.05, label="Contrast")
                saturation = gr.Slider(0.0, 3.0, value=1.0, step=0.05, label="Saturation")
                sharpness = gr.Slider(0.0, 4.0, value=1.0, step=0.1, label="Sharpness")
                blur = gr.Slider(0.0, 8.0, value=0.0, step=0.25, label="Blur")
            with gr.Column():
                resize_percent = gr.Slider(10, 200, value=100, step=5, label="Resize %")
                auto_enhance = gr.Checkbox(label="Auto Enhance", value=False)
                detail_boost = gr.Checkbox(label="Detail Boost", value=False)
                stylize_edges = gr.Checkbox(label="Edge Stylize", value=False)
                portrait_blur = gr.Checkbox(label="Portrait Background Blur", value=False)

        gr.Markdown(
            """
            ## AI Assist
            Use a local prompt like `remove the girl in the background`, `make this photo taken at night`,
            or `add a pear with eyes and a mouth on top of this table`.
            For object removal, place an approximate region box around the thing you want gone.
            For drawing onto a photo, the region box acts like placement and size.
            """
        )

        with gr.Row():
            prompt = gr.Textbox(
                label="Edit Prompt",
                placeholder="remove the girl in the background",
            )

        gr.Markdown(
            """
            ## Drawing Studio
            Create a standalone cartoon drawing with prompts like `draw me a lightbulb with eyes`
            or place one into a photo with `add a pear with eyes and a mouth on top of this table`.
            """
        )

        with gr.Row():
            drawing_prompt = gr.Textbox(
                label="Drawing Prompt",
                placeholder="draw me a lightbulb with eyes",
            )
            drawing_button = gr.Button("Generate Drawing")

        with gr.Row():
            region_x = gr.Slider(0, 4096, value=0, step=1, label="Region X")
            region_y = gr.Slider(0, 4096, value=0, step=1, label="Region Y")
            region_width = gr.Slider(0, 4096, value=0, step=1, label="Region Width")
            region_height = gr.Slider(0, 4096, value=0, step=1, label="Region Height")

        with gr.Row():
            apply_button = gr.Button("Apply Edits", variant="primary")
            ai_button = gr.Button("Run AI Assist")
            mask_button = gr.Button("Preview Foreground Mask")
            reset_button = gr.Button("Reset Controls")
            export_button = gr.Button("Export PNG")

        export_file = gr.File(label="Exported File")

        apply_inputs = [
            input_image,
            brightness,
            contrast,
            saturation,
            sharpness,
            blur,
            resize_percent,
            auto_enhance,
            stylize_edges,
            detail_boost,
            portrait_blur,
        ]

        apply_button.click(apply_edits, inputs=apply_inputs, outputs=output_image)
        ai_button.click(
            apply_ai_edit,
            inputs=[input_image, prompt, region_x, region_y, region_width, region_height],
            outputs=output_image,
        )
        drawing_button.click(generate_drawing, inputs=drawing_prompt, outputs=output_image)
        mask_button.click(preview_mask, inputs=input_image, outputs=output_image)
        export_button.click(export_image, inputs=output_image, outputs=export_file)
        reset_button.click(
            reset_controls,
            inputs=None,
            outputs=[
                brightness,
                contrast,
                saturation,
                sharpness,
                blur,
                resize_percent,
                auto_enhance,
                stylize_edges,
                detail_boost,
                portrait_blur,
            ],
        )

    return app
