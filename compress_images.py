#!/usr/bin/env python3
import os
import sys
import argparse
import glob
from PIL import Image

def compress_images(src_dir, dest_dir, max_dimension, quality, inplace):
    if not os.path.exists(src_dir):
        print(f"Error: Source directory '{src_dir}' does not exist.")
        sys.exit(1)

    if not inplace and not dest_dir:
        print("Error: You must specify a --dest directory or use --inplace to overwrite files.")
        sys.exit(1)

    if dest_dir:
        os.makedirs(dest_dir, exist_ok=True)

    extensions = ["*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"]
    files = []
    for ext in extensions:
        # Search directly in src_dir
        files.extend(glob.glob(os.path.join(src_dir, ext)))
        # Search recursively (optional but helpful)
        files.extend(glob.glob(os.path.join(src_dir, "**", ext), recursive=True))

    # Remove duplicates
    files = sorted(list(set(files)))

    # Exclude the script itself if it gets matched
    files = [f for f in files if not os.path.samefile(f, __file__) if os.path.exists(f)]

    if not files:
        print(f"No image files found in '{src_dir}'.")
        return

    print(f"Found {len(files)} image files to process.")
    print(f"Settings: max-dimension={max_dimension}px, quality={quality}%")
    print("=" * 50)

    total_orig_size = 0
    total_new_size = 0
    success_count = 0

    for filepath in files:
        orig_size = os.path.getsize(filepath)
        total_orig_size += orig_size
        
        # Determine output path
        if inplace:
            out_filepath = filepath
        else:
            rel_path = os.path.relpath(filepath, src_dir)
            out_filepath = os.path.join(dest_dir, rel_path)
            os.makedirs(os.path.dirname(out_filepath), exist_ok=True)

        try:
            with Image.open(filepath) as img:
                # Convert color spaces if saving as JPEG
                save_format = "PNG" if filepath.lower().endswith(".png") else "JPEG"
                if save_format == "JPEG" and img.mode in ("RGBA", "LA", "P"):
                    img = img.convert("RGB")
                elif img.mode == "CMYK":
                    img = img.convert("RGB")

                # Resize keeping aspect ratio
                width, height = img.size
                if width > max_dimension or height > max_dimension:
                    if width > height:
                        new_width = max_dimension
                        new_height = int(height * (max_dimension / width))
                    else:
                        new_height = max_dimension
                        new_width = int(width * (max_dimension / height))
                    
                    img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
                    resize_msg = f"({width}x{height} -> {new_width}x{new_height})"
                else:
                    resize_msg = f"({width}x{height}, skipping resize)"

                # Save optimized image
                img.save(out_filepath, save_format, optimize=True, quality=quality)
                
            new_size = os.path.getsize(out_filepath)
            total_new_size += new_size
            success_count += 1
            saved = orig_size - new_size
            print(f"[OK] {os.path.basename(filepath)} {resize_msg}: {orig_size/1024:.1f}KB -> {new_size/1024:.1f}KB (Saved {saved/1024:.1f}KB)")
        except Exception as e:
            print(f"[ERROR] Failed to process {filepath}: {e}")

    saved_total = total_orig_size - total_new_size
    print("=" * 50)
    print(f"Successfully processed {success_count}/{len(files)} files.")
    print(f"Total Original Size: {total_orig_size/(1024*1024):.2f} MB")
    print(f"Total Optimized Size: {total_new_size/(1024*1024):.2f} MB")
    print(f"Space Saved: {saved_total/(1024*1024):.2f} MB ({(saved_total/total_orig_size)*100:.1f}% reduction)")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Batch Compress and Resize Images")
    parser.add_argument("-s", "--src", default=".", help="Source directory containing images (default: current directory)")
    parser.add_argument("-d", "--dest", help="Destination directory to save compressed images")
    parser.add_argument("-m", "--max-dim", type=int, default=800, help="Maximum dimension (width or height) to resize to (default: 800)")
    parser.add_argument("-q", "--quality", type=int, default=85, help="Image quality (1-100, default: 85)")
    parser.add_argument("--inplace", action="store_true", help="Overwrite the source images directly (warning: this will modify original files)")

    args = parser.parse_args()
    
    # Simple warning for inplace operation
    if args.inplace:
        confirm = input("WARNING: --inplace will overwrite original files. Are you sure? (y/n): ")
        if confirm.lower() != 'y':
            print("Aborted.")
            sys.exit(0)

    compress_images(args.src, args.dest, args.max_dim, args.quality, args.inplace)
