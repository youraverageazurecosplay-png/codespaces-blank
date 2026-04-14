# Echo

Echo is an offline-first AI photo editing project. This first version gives you a local editor with real image-processing tools, a clean architecture, and room to add heavier on-device AI models later without rewriting the app.

## What Echo can do today

- Open images locally in a browser-based interface
- Apply brightness, contrast, saturation, sharpness, and blur adjustments
- Resize images with aspect-ratio aware controls
- Run one-click auto enhancement
- Generate a foreground mask with GrabCut
- Create portrait-style background blur from the generated mask
- Apply edge stylization and detail enhancement
- Generate standalone cartoon drawings from local prompts
- Place supported drawn characters onto an existing photo
- Use prompt-style offline edits for object removal inside a selected region
- Restyle a photo into a nighttime scene with a local night filter
- Export the edited result as PNG

## Tech stack

- Python 3.10+
- Gradio for the local UI
- Pillow for image editing
- OpenCV for offline computer-vision tooling
- NumPy for image operations

## Project layout

```text
Echo/
├── app.py
├── requirements.txt
└── src/
    └── echo/
        ├── __init__.py
        ├── editor.py
        └── pipelines.py
```

## Run locally

1. Create a virtual environment:

```bash
python -m venv .venv
source .venv/bin/activate
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Start Echo:

```bash
python app.py
```

4. Open the local Gradio URL shown in the terminal.

## Roadmap ideas

- On-device inpainting
- Local segmentation model integration
- Layer system and masks
- Prompt-based local edits with diffusion pipelines
- Batch workflows and presets

## Notes

Echo is designed to stay offline by default. The current features do not require cloud APIs.

## Prompt examples

- `remove the girl in the background`
- `remove the car on the left`
- `make this photo taken at night`
- `turn this into a nighttime street scene`
- `add a pear with eyes and a mouth on top of this table`
- `draw me a lightbulb with eyes`
