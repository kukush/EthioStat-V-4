#!/usr/bin/env python3
"""
Generate 5 professional Geometric minimalist app icons for 'EthioStat'
Navy blue (#1E3A5F) + Electric lime green (#C8F569) palette
"""

from PIL import Image, ImageDraw, ImageFont
import math
import os

def create_logo(variant=1, size=512):
    """Create a single logo variant"""
    
    # Colors
    NAVY = "#1E3A5F"
    LIME = "#C8F569"
    WHITE = "#FFFFFF"
    
    # Create base image with transparent background
    img = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    center = size // 2
    scale = size / 512  # Base scale factor
    
    # Background circle (navy)
    bg_margin = int(20 * scale)
    bg_box = [bg_margin, bg_margin, size - bg_margin, size - bg_margin]
    draw.ellipse(bg_box, fill=NAVY)
    
    if variant == 1:
        # Variant 1: Classic €S with integrated upward arrow
        # Draw circular path (connectivity)
        path_radius = int(140 * scale)
        path_width = int(12 * scale)
        
        # Draw partial circle arc (270 degrees, leaving gap for arrow)
        arc_box = [
            center - path_radius, center - path_radius,
            center + path_radius, center + path_radius
        ]
        draw.arc(arc_box, start=45, end=315, fill=LIME, width=path_width)
        
        # E-style curve (left side)
        e_radius = int(100 * scale)
        e_center = (center - 30, center)
        for i, offset in enumerate([-40, 0, 40]):
            y = e_center[1] + int(offset * scale)
            line_x1 = e_center[0] - int(60 * scale)
            line_x2 = e_center[0] + int(40 * scale)
            draw.line([(line_x1, y), (line_x2, y)], fill=LIME, width=path_width)
        
        # Vertical spine of E
        draw.line([
            (e_center[0] - int(60*scale), e_center[1] - int(40*scale)),
            (e_center[0] - int(60*scale), e_center[1] + int(40*scale))
        ], fill=LIME, width=path_width)
        
        # Upward trending arrow (transforms from one end)
        arrow_start = (center + int(100*scale), center + int(80*scale))
        arrow_mid = (center + int(60*scale), center - int(20*scale))
        arrow_end = (center + int(20*scale), center - int(60*scale))
        
        # Arrow shaft
        draw.line([arrow_start, arrow_mid, arrow_end], fill=LIME, width=path_width)
        
        # Arrow head
        head_size = int(25 * scale)
        draw.polygon([
            (arrow_end[0], arrow_end[1] - head_size),
            (arrow_end[0] - head_size//2, arrow_end[1] + head_size//3),
            (arrow_end[0] + head_size//2, arrow_end[1] + head_size//3)
        ], fill=LIME)
        
    elif variant == 2:
        # Variant 2: Stylized ES monogram with arrow
        # E on left
        e_x = center - int(50 * scale)
        bar_width = int(15 * scale)
        bar_length = int(70 * scale)
        
        # E bars
        for i, y_offset in enumerate([-35, 0, 35]):
            y = center + int(y_offset * scale)
            draw.line([
                (e_x - bar_length//2, y),
                (e_x + bar_length//2, y)
            ], fill=LIME, width=bar_width)
        
        # E vertical
        draw.line([
            (e_x - bar_length//2, center - int(35*scale)),
            (e_x - bar_length//2, center + int(35*scale))
        ], fill=LIME, width=bar_width)
        
        # S curves on right
        s_x = center + int(50 * scale)
        
        # Top curve of S
        for angle in range(180, 360, 5):
            rad = math.radians(angle)
            x = s_x + int(40 * scale * math.cos(rad))
            y = center - int(20 * scale) + int(20 * scale * math.sin(rad))
            draw.ellipse([x-8, y-8, x+8, y+8], fill=LIME)
        
        # Bottom curve of S  
        for angle in range(0, 180, 5):
            rad = math.radians(angle)
            x = s_x + int(40 * scale * math.cos(rad))
            y = center + int(20 * scale) + int(20 * scale * math.sin(rad))
            draw.ellipse([x-8, y-8, x+8, y+8], fill=LIME)
        
        # Upward arrow from bottom right
        arrow_base = (center + int(120*scale), center + int(100*scale))
        arrow_tip = (center + int(60*scale), center - int(40*scale))
        
        draw.line([arrow_base, arrow_tip], fill=LIME, width=bar_width)
        # Arrow head
        draw.polygon([
            arrow_tip,
            (arrow_tip[0] - int(20*scale), arrow_tip[1] + int(25*scale)),
            (arrow_tip[0] + int(20*scale), arrow_tip[1] + int(25*scale))
        ], fill=LIME)
        
    elif variant == 3:
        # Variant 3: Circular data flow with integrated growth arrow
        # Outer ring
        outer_r = int(160 * scale)
        draw.arc([
            center - outer_r, center - outer_r,
            center + outer_r, center + outer_r
        ], start=0, end=270, fill=LIME, width=path_width)
        
        # Inner € symbol simplified
        inner_r = int(100 * scale)
        # C shape for euro
        draw.arc([
            center - inner_r, center - int(80*scale),
            center + inner_r, center + int(80*scale)
        ], start=90, end=270, fill=LIME, width=path_width)
        
        # Euro bars
        for y_off in [-30, 0, 30]:
            y = center + int(y_off * scale)
            draw.line([
                (center - int(20*scale), y),
                (center + int(60*scale), y)
            ], fill=LIME, width=path_width)
        
        # Growth arrow breaking out of circle at top right
        arrow_start = (center + int(140*scale), center + int(50*scale))
        arrow_end = (center + int(60*scale), center - int(120*scale))
        
        draw.line([arrow_start, arrow_end], fill=LIME, width=path_width)
        
        # Arrowhead
        draw.polygon([
            arrow_end,
            (arrow_end[0] - int(20*scale), arrow_end[1] + int(30*scale)),
            (arrow_end[0] + int(10*scale), arrow_end[1] + int(30*scale))
        ], fill=LIME)
        
    elif variant == 4:
        # Variant 4: Hexagonal tech-inspired
        # Hexagon shape
        hex_r = int(180 * scale)
        hex_points = []
        for i in range(6):
            angle = math.radians(60 * i - 30)
            x = center + int(hex_r * math.cos(angle))
            y = center + int(hex_r * math.sin(angle))
            hex_points.append((x, y))
        
        draw.polygon(hex_points, outline=LIME, width=path_width)
        
        # E letter inside
        e_box = [
            (center - int(40*scale), center - int(50*scale)),
            (center + int(40*scale), center + int(50*scale))
        ]
        # E vertical
        draw.line([
            (e_box[0][0], e_box[0][1]),
            (e_box[0][0], e_box[1][1])
        ], fill=LIME, width=bar_width)
        
        # E horizontal bars
        for y in [e_box[0][1], center, e_box[1][1]]:
            draw.line([
                (e_box[0][0], y),
                (e_box[1][0], y)
            ], fill=LIME, width=bar_width)
        
        # S flowing into upward arrow on right side
        s_start = (center + int(60*scale), center + int(40*scale))
        s_curve = (center + int(100*scale), center)
        arrow_tip = (center + int(60*scale), center - int(80*scale))
        
        draw.line([s_start, s_curve, arrow_tip], fill=LIME, width=path_width)
        
        # S curve decoration
        draw.arc([
            center + int(80*scale), center - int(20*scale),
            center + int(120*scale), center + int(20*scale)
        ], start=0, end=180, fill=LIME, width=path_width)
        
        # Arrow head
        draw.polygon([
            arrow_tip,
            (arrow_tip[0] - int(15*scale), arrow_tip[1] + int(25*scale)),
            (arrow_tip[0] + int(15*scale), arrow_tip[1] + int(25*scale))
        ], fill=LIME)
        
    else:
        # Variant 5: Minimalist dot-line connection pattern
        # Central node
        node_r = int(20 * scale)
        draw.ellipse([
            center - node_r, center - node_r,
            center + node_r, center + node_r
        ], fill=LIME)
        
        # Surrounding data points
        num_points = 6
        point_r = int(12 * scale)
        radius = int(140 * scale)
        
        points = []
        for i in range(num_points):
            angle = math.radians(360 * i / num_points - 90)
            x = center + int(radius * math.cos(angle))
            y = center + int(radius * math.sin(angle))
            points.append((x, y))
            draw.ellipse([
                x - point_r, y - point_r,
                x + point_r, y + point_r
            ], fill=LIME)
        
        # Connect points with lines
        for i, p1 in enumerate(points):
            p2 = points[(i + 1) % len(points)]
            draw.line([p1, p2], fill=LIME, width=int(6 * scale))
            # Connect to center
            draw.line([p1, (center, center)], fill=LIME, width=int(4 * scale))
        
        # E letter formed by top-left connections
        # Highlight top path as arrow
        top_idx = 0
        top_point = points[top_idx]
        
        # Draw upward arrow from center through top point
        arrow_vec = (top_point[0] - center, top_point[1] - center)
        arrow_end = (
            top_point[0] + int(arrow_vec[0] * 0.3),
            top_point[1] + int(arrow_vec[1] * 0.3)
        )
        
        # Thicken the top connection as growth arrow
        draw.line([center, top_point], fill=LIME, width=int(15 * scale))
        draw.line([top_point, arrow_end], fill=LIME, width=int(15 * scale))
        
        # Arrow head
        head_size = int(30 * scale)
        draw.polygon([
            (arrow_end[0], arrow_end[1] - head_size),
            (arrow_end[0] - head_size//2, arrow_end[1]),
            (arrow_end[0] + head_size//2, arrow_end[1])
        ], fill=LIME)
        
        # Small E letter in center
        try:
            font_size = int(60 * scale)
            font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", font_size)
        except:
            font = ImageFont.load_default()
        
        # Just center dot representing E start
        draw.ellipse([
            center - int(8*scale), center - int(8*scale),
            center + int(8*scale), center + int(8*scale)
        ], fill=NAVY)
    
    return img

def main():
    # Create output directory
    output_dir = "logos"
    os.makedirs(output_dir, exist_ok=True)
    
    print("Generating 5 EthioStat logo variants...")
    
    # Generate 5 variants at Play Store sizes
    for variant in range(1, 6):
        # High-res for Play Store
        logo_hires = create_logo(variant, size=512)
        logo_hires.save(f"{output_dir}/ethiostat_logo_v{variant}_512.png")
        
        # Standard launcher size
        logo_launcher = create_logo(variant, size=192)
        logo_launcher.save(f"{output_dir}/ethiostat_logo_v{variant}_192.png")
        
        # Adaptive icon foreground size
        logo_foreground = create_logo(variant, size=108)
        logo_foreground.save(f"{output_dir}/ethiostat_logo_v{variant}_108.png")
        
        print(f"  ✓ Variant {variant} generated")
    
    print(f"\nLogos saved to: {os.path.abspath(output_dir)}/")
    print("Files created:")
    for f in sorted(os.listdir(output_dir)):
        print(f"  - {f}")

if __name__ == "__main__":
    main()
