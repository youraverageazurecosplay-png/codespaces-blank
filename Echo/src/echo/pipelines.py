from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps


def _to_pil(image: Image.Image | np.ndarray) -> Image.Image:
    if isinstance(image, Image.Image):
        return image.convert("RGB")
    if image is None:
        raise ValueError("No image was provided.")
    return Image.fromarray(image.astype(np.uint8)).convert("RGB")


def _to_cv(image: Image.Image | np.ndarray) -> np.ndarray:
    pil_image = _to_pil(image)
    rgb = np.array(pil_image)
    return cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)


def _cv_to_pil(image: np.ndarray) -> Image.Image:
    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    return Image.fromarray(rgb)


def _contains_any(text: str, words: tuple[str, ...]) -> bool:
    return any(word in text for word in words)


@dataclass
class EditSettings:
    brightness: float
    contrast: float
    saturation: float
    sharpness: float
    blur: float
    resize_percent: int
    auto_enhance: bool
    stylize_edges: bool
    detail_boost: bool
    portrait_blur: bool


class EchoPipeline:
    def basic_adjust(self, image: Image.Image | np.ndarray, settings: EditSettings) -> Image.Image:
        pil_image = _to_pil(image)

        if settings.auto_enhance:
            pil_image = ImageOps.autocontrast(pil_image)

        pil_image = ImageEnhance.Brightness(pil_image).enhance(settings.brightness)
        pil_image = ImageEnhance.Contrast(pil_image).enhance(settings.contrast)
        pil_image = ImageEnhance.Color(pil_image).enhance(settings.saturation)
        pil_image = ImageEnhance.Sharpness(pil_image).enhance(settings.sharpness)

        if settings.blur > 0:
            pil_image = pil_image.filter(ImageFilter.GaussianBlur(radius=settings.blur))

        if settings.resize_percent != 100:
            width, height = pil_image.size
            scale = max(settings.resize_percent, 1) / 100.0
            pil_image = pil_image.resize(
                (max(int(width * scale), 1), max(int(height * scale), 1)),
                Image.Resampling.LANCZOS,
            )

        cv_image = _to_cv(pil_image)

        if settings.detail_boost:
            cv_image = self.detail_boost(cv_image)

        if settings.stylize_edges:
            cv_image = self.edge_stylize(cv_image)

        if settings.portrait_blur:
            mask = self.foreground_mask(cv_image)
            cv_image = self.blur_background(cv_image, mask)

        return _cv_to_pil(cv_image)

    def detail_boost(self, image: np.ndarray) -> np.ndarray:
        return cv2.detailEnhance(image, sigma_s=12, sigma_r=0.15)

    def edge_stylize(self, image: np.ndarray) -> np.ndarray:
        return cv2.stylization(image, sigma_s=40, sigma_r=0.18)

    def foreground_mask(self, image: Image.Image | np.ndarray) -> np.ndarray:
        cv_image = _to_cv(image)
        height, width = cv_image.shape[:2]
        rect = (
            max(1, width // 12),
            max(1, height // 12),
            max(1, width - (width // 6)),
            max(1, height - (height // 6)),
        )

        mask = np.zeros(cv_image.shape[:2], np.uint8)
        bg_model = np.zeros((1, 65), np.float64)
        fg_model = np.zeros((1, 65), np.float64)

        cv2.grabCut(cv_image, mask, rect, bg_model, fg_model, 5, cv2.GC_INIT_WITH_RECT)
        foreground = np.where(
            (mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD),
            255,
            0,
        ).astype("uint8")
        return foreground

    def blur_background(self, image: Image.Image | np.ndarray, mask: np.ndarray | None = None) -> np.ndarray:
        cv_image = _to_cv(image)
        if mask is None:
            mask = self.foreground_mask(cv_image)

        softened_mask = cv2.GaussianBlur(mask, (0, 0), sigmaX=9, sigmaY=9)
        softened_mask = softened_mask.astype(np.float32) / 255.0
        softened_mask = softened_mask[..., None]

        blurred = cv2.GaussianBlur(cv_image, (0, 0), sigmaX=14, sigmaY=14)
        blended = (cv_image * softened_mask + blurred * (1.0 - softened_mask)).astype(np.uint8)
        return blended

    def mask_preview(self, image: Image.Image | np.ndarray) -> Image.Image:
        mask = self.foreground_mask(image)
        return Image.fromarray(mask).convert("L")

    def remove_object_with_box(
        self,
        image: Image.Image | np.ndarray,
        x: int,
        y: int,
        width: int,
        height: int,
        feather: int = 10,
    ) -> Image.Image:
        cv_image = _to_cv(image)
        image_height, image_width = cv_image.shape[:2]

        x = int(np.clip(x, 0, image_width - 1))
        y = int(np.clip(y, 0, image_height - 1))
        width = int(np.clip(width, 1, image_width - x))
        height = int(np.clip(height, 1, image_height - y))

        rect = (x, y, width, height)
        mask = np.zeros(cv_image.shape[:2], np.uint8)
        bg_model = np.zeros((1, 65), np.float64)
        fg_model = np.zeros((1, 65), np.float64)

        cv2.grabCut(cv_image, mask, rect, bg_model, fg_model, 5, cv2.GC_INIT_WITH_RECT)
        object_mask = np.where(
            (mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD),
            255,
            0,
        ).astype("uint8")

        x2 = min(x + width, image_width)
        y2 = min(y + height, image_height)
        constrained_mask = np.zeros_like(object_mask)
        constrained_mask[y:y2, x:x2] = object_mask[y:y2, x:x2]

        kernel_size = max(3, feather * 2 + 1)
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (kernel_size, kernel_size))
        constrained_mask = cv2.dilate(constrained_mask, kernel, iterations=1)

        inpainted = cv2.inpaint(cv_image, constrained_mask, 5, cv2.INPAINT_TELEA)
        return _cv_to_pil(inpainted)

    def night_filter(self, image: Image.Image | np.ndarray, intensity: float = 0.7) -> Image.Image:
        cv_image = _to_cv(image).astype(np.float32)
        height, width = cv_image.shape[:2]

        # Darken overall exposure and cool the tones for a moonlit feel.
        darkness = float(np.clip(intensity, 0.1, 1.0))
        blue_boost = 1.0 + (0.25 * darkness)
        green_boost = 1.0 + (0.05 * darkness)
        red_reduction = 1.0 - (0.30 * darkness)

        cv_image[..., 0] *= blue_boost
        cv_image[..., 1] *= green_boost
        cv_image[..., 2] *= red_reduction
        cv_image *= 1.0 - (0.45 * darkness)

        # Preserve some bright practical lights so windows and lamps still glow.
        luminance = cv2.cvtColor(np.clip(cv_image, 0, 255).astype(np.uint8), cv2.COLOR_BGR2GRAY)
        highlights = cv2.GaussianBlur((luminance > 170).astype(np.float32), (0, 0), sigmaX=7, sigmaY=7)
        highlight_color = np.zeros_like(cv_image)
        highlight_color[..., 1] = 65
        highlight_color[..., 2] = 115
        cv_image = cv_image + (highlight_color * highlights[..., None] * 0.65)

        # Add a vignette so the frame feels more naturally nighttime.
        y_grid, x_grid = np.indices((height, width))
        x_norm = (x_grid - width / 2) / max(width / 2, 1)
        y_norm = (y_grid - height / 2) / max(height / 2, 1)
        distance = np.sqrt(x_norm**2 + y_norm**2)
        vignette = np.clip(1.15 - distance * (0.65 + 0.2 * darkness), 0.35, 1.0)
        cv_image *= vignette[..., None]

        return _cv_to_pil(np.clip(cv_image, 0, 255).astype(np.uint8))

    def _draw_face(self, draw: ImageDraw.ImageDraw, center_x: int, center_y: int, scale: int) -> None:
        eye_w = max(scale // 6, 8)
        eye_h = max(scale // 4, 14)
        left_eye = (
            center_x - scale // 2,
            center_y - eye_h // 2,
            center_x - scale // 2 + eye_w,
            center_y + eye_h // 2,
        )
        right_eye = (
            center_x + scale // 2 - eye_w,
            center_y - eye_h // 2,
            center_x + scale // 2,
            center_y + eye_h // 2,
        )
        draw.ellipse(left_eye, fill="white")
        draw.ellipse(right_eye, fill="white")

        pupil_w = max(eye_w // 2, 4)
        pupil_h = max(eye_h // 2, 6)
        draw.ellipse(
            (
                left_eye[0] + eye_w // 3,
                left_eye[1] + eye_h // 3,
                left_eye[0] + eye_w // 3 + pupil_w,
                left_eye[1] + eye_h // 3 + pupil_h,
            ),
            fill="black",
        )
        draw.ellipse(
            (
                right_eye[0] + eye_w // 3,
                right_eye[1] + eye_h // 3,
                right_eye[0] + eye_w // 3 + pupil_w,
                right_eye[1] + eye_h // 3 + pupil_h,
            ),
            fill="black",
        )
        draw.arc(
            (
                center_x - scale // 2,
                center_y + scale // 4,
                center_x + scale // 2,
                center_y + scale,
            ),
            start=10,
            end=170,
            fill=(70, 35, 35, 255),
            width=max(scale // 10, 3),
        )

    def _pear_sticker(self, size: int) -> Image.Image:
        sticker = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(sticker)
        body_color = (166, 205, 72, 255)
        shadow_color = (111, 145, 40, 255)

        draw.ellipse((size * 0.26, size * 0.12, size * 0.74, size * 0.55), fill=body_color)
        draw.ellipse((size * 0.14, size * 0.32, size * 0.86, size * 0.94), fill=body_color)
        draw.ellipse((size * 0.18, size * 0.55, size * 0.82, size * 0.93), fill=shadow_color)

        draw.rounded_rectangle(
            (size * 0.47, size * 0.01, size * 0.53, size * 0.18),
            radius=size * 0.03,
            fill=(105, 72, 40, 255),
        )
        draw.polygon(
            [
                (size * 0.51, size * 0.09),
                (size * 0.73, size * 0.03),
                (size * 0.80, size * 0.16),
                (size * 0.59, size * 0.18),
            ],
            fill=(84, 148, 59, 255),
        )

        self._draw_face(draw, size // 2, int(size * 0.48), max(size // 5, 24))
        return sticker

    def _lightbulb_sticker(self, size: int) -> Image.Image:
        sticker = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(sticker)

        bulb_box = (size * 0.18, size * 0.10, size * 0.82, size * 0.72)
        draw.ellipse(bulb_box, fill=(255, 241, 153, 255), outline=(209, 170, 73, 255), width=max(size // 40, 3))
        draw.rounded_rectangle(
            (size * 0.35, size * 0.65, size * 0.65, size * 0.82),
            radius=size * 0.03,
            fill=(168, 168, 174, 255),
        )
        for offset in (0.68, 0.73, 0.78):
            draw.line(
                (size * 0.37, size * offset, size * 0.63, size * offset),
                fill=(115, 115, 122, 255),
                width=max(size // 50, 2),
            )

        self._draw_face(draw, size // 2, int(size * 0.38), max(size // 5, 24))

        for angle in range(0, 360, 45):
            radians = np.deg2rad(angle)
            inner = (size * 0.5 + np.cos(radians) * size * 0.34, size * 0.38 + np.sin(radians) * size * 0.34)
            outer = (size * 0.5 + np.cos(radians) * size * 0.46, size * 0.38 + np.sin(radians) * size * 0.46)
            draw.line((*inner, *outer), fill=(255, 214, 82, 255), width=max(size // 40, 3))

        return sticker

    def _pick_subject(self, prompt: str) -> str:
        if "pear" in prompt:
            return "pear"
        if "lightbulb" in prompt or "bulb" in prompt:
            return "lightbulb"
        raise ValueError("Echo drawing currently supports a pear or a lightbulb character.")

    def draw_subject(self, prompt: str, size: int = 512) -> Image.Image:
        normalized = (prompt or "").strip().lower()
        if not normalized:
            raise ValueError("Enter a drawing prompt for Echo.")

        subject = self._pick_subject(normalized)
        if subject == "pear":
            sticker = self._pear_sticker(size)
            background = Image.new("RGBA", (size, size), (246, 251, 234, 255))
        else:
            sticker = self._lightbulb_sticker(size)
            background = Image.new("RGBA", (size, size), (241, 247, 255, 255))

        shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        shadow_draw = ImageDraw.Draw(shadow)
        shadow_draw.ellipse(
            (size * 0.24, size * 0.78, size * 0.76, size * 0.92),
            fill=(0, 0, 0, 70),
        )
        shadow = shadow.filter(ImageFilter.GaussianBlur(radius=max(size // 25, 8)))
        composed = Image.alpha_composite(background, shadow)
        return Image.alpha_composite(composed, sticker).convert("RGB")

    def add_drawn_subject(
        self,
        image: Image.Image | np.ndarray,
        prompt: str,
        x: int = 0,
        y: int = 0,
        size: int = 180,
    ) -> Image.Image:
        base = _to_pil(image).convert("RGBA")
        normalized = (prompt or "").strip().lower()
        subject = self._pick_subject(normalized)

        sticker = self._pear_sticker(512) if subject == "pear" else self._lightbulb_sticker(512)
        sticker = ImageOps.contain(sticker, (max(size, 48), max(size, 48)))

        if x <= 0:
            x = max((base.width - sticker.width) // 2, 0)
        if y <= 0:
            y = max(base.height - sticker.height - max(base.height // 12, 16), 0)

        x = int(np.clip(x, 0, max(base.width - sticker.width, 0)))
        y = int(np.clip(y, 0, max(base.height - sticker.height, 0)))

        shadow = Image.new("RGBA", base.size, (0, 0, 0, 0))
        shadow_blob = Image.new("RGBA", sticker.size, (0, 0, 0, 0))
        shadow_draw = ImageDraw.Draw(shadow_blob)
        shadow_draw.ellipse(
            (sticker.width * 0.12, sticker.height * 0.82, sticker.width * 0.88, sticker.height * 0.98),
            fill=(0, 0, 0, 95),
        )
        shadow_blob = shadow_blob.filter(ImageFilter.GaussianBlur(radius=max(sticker.width // 18, 6)))
        shadow.alpha_composite(shadow_blob, (x, y))

        composed = Image.alpha_composite(base, shadow)
        composed.alpha_composite(sticker, (x, y))
        return composed.convert("RGB")

    def prompt_edit(
        self,
        image: Image.Image | np.ndarray,
        prompt: str,
        x: int = 0,
        y: int = 0,
        width: int = 0,
        height: int = 0,
    ) -> Image.Image:
        normalized = (prompt or "").strip().lower()
        if not normalized:
            raise ValueError("Enter a prompt so Echo knows which offline edit to apply.")

        if "night" in normalized or "moonlight" in normalized or "evening" in normalized:
            return self.night_filter(image)

        drawing_words = ("draw", "add", "put", "place")
        if _contains_any(normalized, drawing_words):
            size = max(min(width if width > 0 else 180, 512), 64)
            return self.add_drawn_subject(image, normalized, x, y, size)

        removal_words = ("remove", "erase", "delete", "clear")
        if any(word in normalized for word in removal_words):
            if width <= 0 or height <= 0:
                raise ValueError("Set a region box before asking Echo to remove an object.")
            return self.remove_object_with_box(image, x, y, width, height)

        raise ValueError(
            "Echo currently supports drawing a pear or lightbulb character, object removal, and nighttime restyling."
        )

    def export_png_bytes(self, image: Image.Image | np.ndarray) -> bytes:
        output = BytesIO()
        _to_pil(image).save(output, format="PNG")
        return output.getvalue()
