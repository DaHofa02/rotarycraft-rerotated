#!/usr/bin/env python3
"""Kinetic cell textures for rotaryme, derived from Applied Energistics 2's item cell art.

AE2's textures are (c) 2020 Ridanisaurus Rid, (c) 2013-2020 AlgorithmX2 et al, licensed CC BY-NC-SA 3.0
(https://creativecommons.org/licenses/by-nc-sa/3.0/). Changes made here: the coloured accent stripe is painted
black (dark and mid tone), only its highlight pixels keep a tier colour. The results are under the same licence
(see LICENSE-AE2-ART.txt next to them).

Usage: gen_kinetic_cell_art.py <ae2 jar> <rotaryme assets dir>
The status LED is not copied: the cell models use AE2's own ae2:item/storage_cell_led as layer1.
"""
import io
import sys
import zipfile

from PIL import Image

DARK, MID = (0x14, 0x14, 0x18), (0x26, 0x26, 0x2c)
TIERS = ['1k', '4k', '16k', '64k', '256k']


def load(z, name):
	return Image.open(io.BytesIO(z.read('assets/ae2/textures/item/'+name))).convert('RGBA')


def tones(im, base):
	"""The accent pixels (where a cell differs from the empty housing), grouped by colour: dark, mid, light by count."""
	count = {}
	for y in range(16):
		for x in range(16):
			p = im.getpixel((x, y))
			if p != base.getpixel((x, y)):
				count[p] = count.get(p, 0)+1
	return sorted(count, key=lambda c: -count[c])


def recolour(im, mapping):
	out = im.copy()
	for y in range(16):
		for x in range(16):
			p = im.getpixel((x, y))
			if p in mapping:
				out.putpixel((x, y), mapping[p])
	return out


def main(jar, assets):
	z = zipfile.ZipFile(jar)
	base = load(z, 'item_cell_housing.png')
	tex = assets+'/textures/item/'
	for tier in TIERS:
		im = load(z, 'item_storage_cell_%s.png' % tier)
		dark, mid, light = tones(im, base)
		recolour(im, {dark: DARK+(255,), mid: MID+(255,)}).save(tex+'kinetic_cell_%s.png' % tier)
	#housing: the empty accent slot of AE2's housing, painted black in the same pattern as a cell
	cell = load(z, 'item_storage_cell_1k.png')
	dark, mid, light = tones(cell, base)
	out = base.copy()
	for y in range(16):
		for x in range(16):
			c = cell.getpixel((x, y))
			if c != base.getpixel((x, y)):
				out.putpixel((x, y), (DARK if c == dark else MID)+(255,))
	out.save(tex+'kinetic_cell_housing.png')


if __name__ == '__main__':
	main(sys.argv[1], sys.argv[2])
