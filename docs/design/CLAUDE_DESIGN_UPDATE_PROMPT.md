# Repz — Design update: new logo & brand colour

> Paste everything below the line into Claude Design, together with the attached files listed in §0.

---

## 0. What changed and what you must do

Two brand decisions changed after your first pass:

1. **New logo:** a dumbbell symbol + rounded "repz" wordmark. It **replaces** the Cookie-R mark everywhere.
2. **New brand colour:** **Electric cyan `#00C2E0`** replaces electric violet `#6B3CF5` as the seed/brand colour.

Update the existing Repz canvas (all 19 boards) accordingly. **Change only what this brief asks for**: layouts, components, typography, motion tokens and illustrations stay as they are unless a section below says otherwise.

Attached files (use them, don't recreate them):
- `brand/LOGO.md`: the logo spec (geometry, colours, usage, motion). It is the single source of truth.
- `brand/logo/master/*.svg`: outlined black masters (also inlined in §1).
- `brand/logo/color/lockup-stacked-on-{ink,cyan,paper}.png`: approved colourways.
- `brand/logo/app-icon/ios-{light,dark,tinted}-1024.png` + `android-{foreground,background,monochrome}-108.svg`: final app icons.
- `brand/logo/motion/splash-storyboard.png`: key frames of the logo animation.

## 1. Logo: use it 1:1, invent nothing

**Hard rules:**
- Use the SVG path data below **exactly as given**. Copy it; do not redraw, trace, simplify, re-space, round differently, change proportions, change the plate count, or set "repz" in any font.
- Only allowed transforms: uniform scale and translate. No rotation, skew, non-uniform scale or cropping.
- Only allowed colourways: the ones in §3 (one colour per mark). No gradients, outlines, shadows, glows, textures or multi-coloured parts (single exception: the PR moment in §4, which recolours the whole symbol to record gold).
- Respect clear space (= outer-collar height, 48 units on the symbol grid) and minimum sizes (symbol 24 dp wide, horizontal lockup 96 dp, stacked lockup 72 dp).
- If a layout does not fit the logo, **change the layout, not the logo**. If you think the logo itself needs a change, **don't make it**; list it as a question at the end of your answer.

**Symbol** (each plate is a separate `<path>` with an id, keep the ids, the animation relies on them):
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 160" width="320" height="160">
  <path id="bar" fill="#000000" d="M110,70 L210,70 A10 10 0 0 1 220,80 L220,80 A10 10 0 0 1 210,90 L110,90 A10 10 0 0 1 100,80 L100,80 A10 10 0 0 1 110,70 Z"/>
  <path id="plate-inner-left" fill="#000000" d="M86,16 L86,16 A14 14 0 0 1 100,30 L100,130 A14 14 0 0 1 86,144 L86,144 A14 14 0 0 1 72,130 L72,30 A14 14 0 0 1 86,16 Z"/>
  <path id="plate-inner-right" fill="#000000" d="M234,16 L234,16 A14 14 0 0 1 248,30 L248,130 A14 14 0 0 1 234,144 L234,144 A14 14 0 0 1 220,130 L220,30 A14 14 0 0 1 234,16 Z"/>
  <path id="plate-middle-left" fill="#000000" d="M52,32 L52,32 A12 12 0 0 1 64,44 L64,116 A12 12 0 0 1 52,128 L52,128 A12 12 0 0 1 40,116 L40,44 A12 12 0 0 1 52,32 Z"/>
  <path id="plate-middle-right" fill="#000000" d="M268,32 L268,32 A12 12 0 0 1 280,44 L280,116 A12 12 0 0 1 268,128 L268,128 A12 12 0 0 1 256,116 L256,44 A12 12 0 0 1 268,32 Z"/>
  <path id="plate-outer-left" fill="#000000" d="M22,56 L22,56 A10 10 0 0 1 32,66 L32,94 A10 10 0 0 1 22,104 L22,104 A10 10 0 0 1 12,94 L12,66 A10 10 0 0 1 22,56 Z"/>
  <path id="plate-outer-right" fill="#000000" d="M298,56 L298,56 A10 10 0 0 1 308,66 L308,94 A10 10 0 0 1 298,104 L298,104 A10 10 0 0 1 288,94 L288,66 A10 10 0 0 1 298,56 Z"/>
</svg>
```

**Wordmark:**
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 350 124" width="350" height="124">
  <path fill="#000000" d="M172,42 L172,11 Q172,8.812 172.837,6.79 Q173.675,4.769 175.222,3.222 Q176.769,1.675 178.79,0.837 Q180.812,0 183,0 Q185.188,0 187.21,0.837 Q189.231,1.675 190.778,3.222 Q191.901,4.344 192.649,5.716 Q202.141,0 214,0 Q231.397,0 243.698,12.302 Q256,24.603 256,42 Q256,59.397 243.699,71.698 Q231.397,84 214,84 Q202.977,84 194,79.062 L194,113 Q194,115.188 193.163,117.21 Q192.325,119.231 190.778,120.778 Q189.231,122.325 187.21,123.163 Q185.188,124 183,124 Q180.812,124 178.79,123.163 Q176.769,122.325 175.222,120.778 Q173.675,119.231 172.837,117.21 Q172,115.188 172,113 L172,42 Z M143.392,53 Q144.792,53.19 146.119,53.741 Q148.14,54.58 149.685,56.129 Q151.231,57.678 152.067,59.7 Q152.902,61.722 152.9,63.91 Q152.898,66.098 152.059,68.119 Q151.22,70.14 149.671,71.685 Q139.034,82.303 124.076,83.761 Q109.118,85.22 96.63,76.857 Q84.143,68.495 79.796,54.108 Q75.448,39.722 81.215,25.843 Q86.982,11.965 100.245,4.896 Q113.507,-2.174 128.244,0.776 Q142.98,3.726 152.5,15.356 Q162.02,26.985 162,42.014 Q161.997,44.201 161.159,46.22 Q160.32,48.238 158.773,49.783 Q157.226,51.328 155.206,52.164 Q153.186,53 151,53 L143.392,53 Z M277,0 L339,0 Q341.188,0 343.209,0.837 Q345.231,1.675 346.778,3.222 Q348.325,4.769 349.163,6.79 Q350,8.812 350,11 Q350,15.556 346.778,18.778 L303.556,62 L339,62 Q341.188,62 343.209,62.837 Q345.231,63.675 346.778,65.222 Q348.325,66.769 349.163,68.79 Q350,70.812 350,73 Q350,75.188 349.163,77.21 Q348.325,79.231 346.778,80.778 Q345.231,82.325 343.209,83.163 Q341.188,84 339,84 L277,84 Q272.444,84 269.222,80.778 Q267.675,79.231 266.837,77.21 Q266,75.188 266,73 Q266,70.812 266.837,68.79 Q267.675,66.769 269.222,65.222 L312.444,22 L277,22 Q274.812,22 272.79,21.163 Q270.769,20.325 269.222,18.778 Q267.675,17.231 266.837,15.21 Q266,13.188 266,11 Q266,8.812 266.837,6.79 Q267.675,4.769 269.222,3.222 Q270.769,1.675 272.79,0.837 Q274.812,0 277,0 Z M0,73 L0,42 Q0,24.603 12.302,12.302 Q24.603,0 42,0 L57,0 Q59.188,0 61.21,0.837 Q63.231,1.675 64.778,3.222 Q66.325,4.769 67.163,6.79 Q68,8.812 68,11 Q68,13.188 67.163,15.21 Q66.325,17.231 64.778,18.778 Q63.231,20.325 61.21,21.163 Q59.188,22 57,22 L42,22 Q33.716,22 27.858,27.858 Q22,33.716 22,42 L22,73 Q22,75.188 21.163,77.21 Q20.325,79.231 18.778,80.778 Q17.231,82.325 15.21,83.163 Q13.188,84 11,84 Q8.812,84 6.79,83.163 Q4.769,82.325 3.222,80.778 Q1.675,79.231 0.837,77.21 Q0,75.188 0,73 Z M194,42 Q194,50.284 199.858,56.142 Q205.716,62 214,62 Q222.284,62 228.142,56.142 Q234,50.284 234,42 Q234,33.716 228.142,27.858 Q222.284,22 214,22 Q205.716,22 199.858,27.858 Q194,33.716 194,42 Z M103.282,53 Q105.426,56.27 108.872,58.577 Q114.818,62.56 121.941,61.865 Q129.064,61.17 134.129,56.115 Q135.678,54.569 137.7,53.733 Q139.018,53.189 140.407,53 L103.282,53 Z M136.741,31 Q136.161,30.127 135.476,29.291 Q130.943,23.753 123.926,22.348 Q116.908,20.944 110.593,24.31 Q105.956,26.781 103.244,31 L136.741,31 Z"/>
</svg>
```

**Stacked lockup:**
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 260" width="320" height="260">
  <path id="bar" fill="#000000" d="M110,70 L210,70 A10 10 0 0 1 220,80 L220,80 A10 10 0 0 1 210,90 L110,90 A10 10 0 0 1 100,80 L100,80 A10 10 0 0 1 110,70 Z"/>
  <path id="plate-inner-left" fill="#000000" d="M86,16 L86,16 A14 14 0 0 1 100,30 L100,130 A14 14 0 0 1 86,144 L86,144 A14 14 0 0 1 72,130 L72,30 A14 14 0 0 1 86,16 Z"/>
  <path id="plate-inner-right" fill="#000000" d="M234,16 L234,16 A14 14 0 0 1 248,30 L248,130 A14 14 0 0 1 234,144 L234,144 A14 14 0 0 1 220,130 L220,30 A14 14 0 0 1 234,16 Z"/>
  <path id="plate-middle-left" fill="#000000" d="M52,32 L52,32 A12 12 0 0 1 64,44 L64,116 A12 12 0 0 1 52,128 L52,128 A12 12 0 0 1 40,116 L40,44 A12 12 0 0 1 52,32 Z"/>
  <path id="plate-middle-right" fill="#000000" d="M268,32 L268,32 A12 12 0 0 1 280,44 L280,116 A12 12 0 0 1 268,128 L268,128 A12 12 0 0 1 256,116 L256,44 A12 12 0 0 1 268,32 Z"/>
  <path id="plate-outer-left" fill="#000000" d="M22,56 L22,56 A10 10 0 0 1 32,66 L32,94 A10 10 0 0 1 22,104 L22,104 A10 10 0 0 1 12,94 L12,66 A10 10 0 0 1 22,56 Z"/>
  <path id="plate-outer-right" fill="#000000" d="M298,56 L298,56 A10 10 0 0 1 308,66 L308,94 A10 10 0 0 1 298,104 L298,104 A10 10 0 0 1 288,94 L288,66 A10 10 0 0 1 298,56 Z"/>
  <path fill="#000000" d="M157.9,197.4 L157.9,175.7 Q157.9,172.511 160.155,170.255 Q162.411,168 165.6,168 Q168.789,168 171.045,170.255 Q171.853,171.063 172.371,171.991 Q179.009,168 187.3,168 Q199.478,168 208.089,176.611 Q216.7,185.222 216.7,197.4 Q216.7,209.578 208.089,218.189 Q199.478,226.8 187.3,226.8 Q179.584,226.8 173.3,223.343 L173.3,247.1 Q173.3,250.289 171.045,252.545 Q168.789,254.8 165.6,254.8 Q162.411,254.8 160.155,252.545 Q157.9,250.289 157.9,247.1 L157.9,197.4 Z M137.924,205.1 Q140.421,205.428 142.28,207.29 Q144.533,209.548 144.53,212.737 Q144.527,215.927 142.27,218.18 Q134.824,225.612 124.353,226.633 Q113.883,227.654 105.141,221.8 Q96.4,215.946 93.357,205.876 Q90.314,195.805 94.351,186.09 Q98.387,176.375 107.671,171.427 Q116.955,166.479 127.271,168.543 Q137.586,170.608 144.25,178.749 Q150.914,186.89 150.9,197.41 Q150.896,200.597 148.641,202.848 Q146.386,205.1 143.2,205.1 L137.924,205.1 Z M231.4,168 L274.8,168 Q277.989,168 280.245,170.255 Q282.5,172.511 282.5,175.7 Q282.5,178.889 280.245,181.145 L249.989,211.4 L274.8,211.4 Q277.989,211.4 280.245,213.655 Q282.5,215.911 282.5,219.1 Q282.5,222.289 280.245,224.545 Q277.989,226.8 274.8,226.8 L231.4,226.8 Q228.211,226.8 225.955,224.545 Q223.7,222.289 223.7,219.1 Q223.7,215.911 225.955,213.655 L256.211,183.4 L231.4,183.4 Q228.211,183.4 225.955,181.145 Q223.7,178.889 223.7,175.7 Q223.7,172.511 225.955,170.255 Q228.211,168 231.4,168 Z M37.5,219.1 L37.5,197.4 Q37.5,185.222 46.111,176.611 Q54.722,168 66.9,168 L77.4,168 Q80.589,168 82.845,170.255 Q85.1,172.511 85.1,175.7 Q85.1,178.889 82.845,181.145 Q80.589,183.4 77.4,183.4 L66.9,183.4 Q61.101,183.4 57.001,187.501 Q52.9,191.601 52.9,197.4 L52.9,219.1 Q52.9,222.289 50.645,224.545 Q48.389,226.8 45.2,226.8 Q42.011,226.8 39.755,224.545 Q37.5,222.289 37.5,219.1 Z M173.3,197.4 Q173.3,203.199 177.4,207.299 Q181.501,211.4 187.3,211.4 Q193.099,211.4 197.199,207.299 Q201.3,203.199 201.3,197.4 Q201.3,191.601 197.2,187.501 Q193.099,183.4 187.3,183.4 Q181.501,183.4 177.4,187.501 Q173.3,191.601 173.3,197.4 Z M133.219,189.7 Q132.812,189.089 132.333,188.504 Q129.16,184.627 124.248,183.644 Q119.336,182.661 114.915,185.017 Q111.669,186.747 109.771,189.7 L133.219,189.7 Z M109.798,205.1 Q111.298,207.389 113.71,209.004 Q117.873,211.792 122.859,211.305 Q127.845,210.819 131.39,207.28 Q133.247,205.427 135.734,205.1 L109.798,205.1 Z"/>
</svg>
```

**Horizontal lockup:**
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 470 116" width="470" height="116">
  <path id="bar" fill="#000000" d="M59,46 L119,46 C122.314,46 125,48.686 125,52 L125,52 C125,55.314 122.314,58 119,58 L59,58 C55.686,58 53,55.314 53,52 L53,52 C53,48.686 55.686,46 59,46 Z"/>
  <path id="plate-inner-left" fill="#000000" d="M44.6,13.6 L44.6,13.6 C49.239,13.6 53,17.361 53,22 L53,82 C53,86.639 49.239,90.4 44.6,90.4 L44.6,90.4 C39.961,90.4 36.2,86.639 36.2,82 L36.2,22 C36.2,17.361 39.961,13.6 44.6,13.6 Z"/>
  <path id="plate-inner-right" fill="#000000" d="M133.4,13.6 L133.4,13.6 C138.039,13.6 141.8,17.361 141.8,22 L141.8,82 C141.8,86.639 138.039,90.4 133.4,90.4 L133.4,90.4 C128.761,90.4 125,86.639 125,82 L125,22 C125,17.361 128.761,13.6 133.4,13.6 Z"/>
  <path id="plate-middle-left" fill="#000000" d="M24.2,23.2 L24.2,23.2 C28.176,23.2 31.4,26.424 31.4,30.4 L31.4,73.6 C31.4,77.576 28.176,80.8 24.2,80.8 L24.2,80.8 C20.224,80.8 17,77.576 17,73.6 L17,30.4 C17,26.424 20.224,23.2 24.2,23.2 Z"/>
  <path id="plate-middle-right" fill="#000000" d="M153.8,23.2 L153.8,23.2 C157.776,23.2 161,26.424 161,30.4 L161,73.6 C161,77.576 157.776,80.8 153.8,80.8 L153.8,80.8 C149.824,80.8 146.6,77.576 146.6,73.6 L146.6,30.4 C146.6,26.424 149.824,23.2 153.8,23.2 Z"/>
  <path id="plate-outer-left" fill="#000000" d="M6.2,37.6 L6.2,37.6 C9.514,37.6 12.2,40.286 12.2,43.6 L12.2,60.4 C12.2,63.714 9.514,66.4 6.2,66.4 L6.2,66.4 C2.886,66.4 0.2,63.714 0.2,60.4 L0.2,43.6 C0.2,40.286 2.886,37.6 6.2,37.6 Z"/>
  <path id="plate-outer-right" fill="#000000" d="M171.8,37.6 L171.8,37.6 C175.114,37.6 177.8,40.286 177.8,43.6 L177.8,60.4 C177.8,63.714 175.114,66.4 171.8,66.4 L171.8,66.4 C168.486,66.4 165.8,63.714 165.8,60.4 L165.8,43.6 C165.8,40.286 168.486,37.6 171.8,37.6 Z"/>
  <path fill="#000000" d="M335,51.5 L335,28.25 Q335,26.609 335.628,25.093 Q336.256,23.577 337.416,22.416 Q338.577,21.256 340.093,20.628 Q341.609,20 343.25,20 Q344.891,20 346.407,20.628 Q347.923,21.256 349.084,22.416 Q349.925,23.258 350.487,24.287 Q357.606,20 366.5,20 Q379.548,20 388.774,29.226 Q398,38.452 398,51.5 Q398,64.548 388.774,73.774 Q379.548,83 366.5,83 Q358.233,83 351.5,79.296 L351.5,104.75 Q351.5,106.391 350.872,107.907 Q350.244,109.423 349.084,110.584 Q347.923,111.744 346.407,112.372 Q344.891,113 343.25,113 Q341.609,113 340.093,112.372 Q338.577,111.744 337.416,110.584 Q336.256,109.423 335.628,107.907 Q335,106.391 335,104.75 L335,51.5 Z M313.544,59.75 Q314.594,59.893 315.589,60.306 Q317.105,60.935 318.264,62.097 Q319.423,63.258 320.05,64.775 Q320.677,66.292 320.675,67.933 Q320.673,69.574 320.044,71.089 Q319.415,72.605 318.253,73.764 Q310.276,81.727 299.057,82.821 Q287.838,83.915 278.473,77.643 Q269.107,71.371 265.847,60.581 Q262.586,49.791 266.911,39.382 Q271.236,28.974 281.183,23.672 Q291.13,18.37 302.183,20.582 Q313.235,22.795 320.375,31.517 Q327.515,40.239 327.5,51.511 Q327.498,53.15 326.869,54.665 Q326.24,56.179 325.08,57.337 Q323.92,58.496 322.405,59.123 Q320.89,59.75 319.25,59.75 L313.544,59.75 Z M413.75,20 L460.25,20 Q461.891,20 463.407,20.628 Q464.923,21.256 466.084,22.416 Q467.244,23.577 467.872,25.093 Q468.5,26.609 468.5,28.25 Q468.5,31.667 466.084,34.084 L433.667,66.5 L460.25,66.5 Q461.891,66.5 463.407,67.128 Q464.923,67.756 466.084,68.916 Q467.244,70.077 467.872,71.593 Q468.5,73.109 468.5,74.75 Q468.5,76.391 467.872,77.907 Q467.244,79.423 466.084,80.584 Q464.923,81.744 463.407,82.372 Q461.891,83 460.25,83 L413.75,83 Q410.333,83 407.916,80.584 Q406.756,79.423 406.128,77.907 Q405.5,76.391 405.5,74.75 Q405.5,73.109 406.128,71.593 Q406.756,70.077 407.916,68.916 L440.333,36.5 L413.75,36.5 Q412.109,36.5 410.593,35.872 Q409.077,35.244 407.916,34.084 Q406.756,32.923 406.128,31.407 Q405.5,29.891 405.5,28.25 Q405.5,26.609 406.128,25.093 Q406.756,23.577 407.916,22.416 Q409.077,21.256 410.593,20.628 Q412.109,20 413.75,20 Z M206,74.75 L206,51.5 Q206,38.452 215.226,29.226 Q224.452,20 237.5,20 L248.75,20 Q250.391,20 251.907,20.628 Q253.423,21.256 254.584,22.416 Q255.744,23.577 256.372,25.093 Q257,26.609 257,28.25 Q257,29.891 256.372,31.407 Q255.744,32.923 254.584,34.084 Q253.423,35.244 251.907,35.872 Q250.391,36.5 248.75,36.5 L237.5,36.5 Q231.287,36.5 226.893,40.893 Q222.5,45.287 222.5,51.5 L222.5,74.75 Q222.5,76.391 221.872,77.907 Q221.244,79.423 220.084,80.584 Q218.923,81.744 217.407,82.372 Q215.891,83 214.25,83 Q212.609,83 211.093,82.372 Q209.577,81.744 208.416,80.584 Q207.256,79.423 206.628,77.907 Q206,76.391 206,74.75 Z M351.5,51.5 Q351.5,57.713 355.893,62.107 Q360.287,66.5 366.5,66.5 Q372.713,66.5 377.107,62.107 Q381.5,57.713 381.5,51.5 Q381.5,45.287 377.107,40.893 Q372.713,36.5 366.5,36.5 Q360.287,36.5 355.893,40.893 Q351.5,45.287 351.5,51.5 Z M283.462,59.75 Q285.07,62.203 287.654,63.933 Q292.114,66.92 297.456,66.399 Q302.798,65.878 306.597,62.086 Q307.758,60.927 309.275,60.3 Q310.264,59.892 311.306,59.75 L283.462,59.75 Z M308.556,43.25 Q308.12,42.595 307.607,41.968 Q304.207,37.815 298.944,36.761 Q293.681,35.708 288.945,38.232 Q285.467,40.086 283.433,43.25 L308.556,43.25 Z"/>
</svg>
```

## 2. Where the logo goes (replace every Cookie-R)

- **App icon & splash board:** replace with the attached icons (iOS light/dark/tinted, Android adaptive foreground/background/monochrome and the themed version, plus circle/squircle mask previews at 108, 48 and 24 dp). Android 12+ splash: `windowSplashScreenBackground` = ink `#0E1416` (night) / paper `#F5FAFB` (day), animated icon = symbol in cyan (night) / deep `#00687A` (day). iOS LaunchScreen: the symbol at 96 pt, centred, same colours, no text.
- **Welcome / Splash screens (A1, A2), Login/Register headers, About & licenses, the overview board:** use the stacked or horizontal lockup.
- **Top app bars:** where a brand mark appeared, use the symbol only.
- Show the **lockups board**: stacked + horizontal on ink, cyan and paper, one-colour black/white, clear-space and minimum-size diagrams, and a "don'ts" row (stretched, recoloured plates, outlined, rotated, set in a font, on busy imagery).

## 3. New colour system (seed `#00C2E0`)

Regenerate the **full Material 3 scheme (light + dark) from seed `#00C2E0`** with Material's colour utilities, as you did before, with every role name, the same table format and the same contrast table.

**Fixed brand values** (these must appear exactly as given):

| Token | Hex | Use |
|---|---|---|
| `brandCyan` | `#00C2E0` | brand field, logo on dark, hero accents in dark theme |
| `brandDeep` | `#00687A` | logo on light; light-theme primary should be this tone or very close (text-safe) |
| `brandInk` | `#0E1416` | dark `background` and `surface` (replaces `#14121C`); logo on cyan |
| `brandPaper` | `#F5FAFB` | light `background` and `surface` (replaces `#FDF7FF`) |
| `brandMist` | `#A6EEFF` | light tint on dark (labels, glows) |

Rules:
- Text or icons **on** cyan fields are **ink (dark)**, never white: white on `#00C2E0` fails WCAG AA.
- Dark-theme surfaces become **cyan-tinted neutrals** built from `#0E1416` (all surfaceContainer levels), no violet tint anywhere.
- **Resolve collisions with the extended colours:**
  - `setWarmup` (currently light blue `#81D1F6` / `#006685`) is too close to cyan. Move it to another hue that is clearly distinct from cyan, success, record and failure (e.g. a muted indigo or slate), then re-check contrast.
  - `chartSeries3` (teal `#00CEC3`) is too close to cyan. Make `chartSeries1` the brand cyan family and re-pick the other two series so all three are distinguishable (also for colour-blind users), with AA contrast on surface.
  - Check that `success` green, `record` gold, `musclePrimary` coral and `error` red still read as distinct from cyan in both themes.
- Show the 3 **dynamic-colour** screens again: what stays fixed (logo, record gold, set-type colours) vs what follows the wallpaper.
- Update the **handoff token table** with the new values, keeping the same token names.

Then **re-colour every board** (foundations, components, all 49 screens, iOS, notifications, accessibility at 200 %, motion storyboards, illustration style guide backgrounds) with the new scheme. Illustrations keep their style; only background/backdrop and any violet accents change.

## 4. Motion: replace MOT-01 and add logo-based motion

Replace MOT-01 with **"Load the bar"** exactly as specified in `brand/LOGO.md` §5:
- 0 ms: bar `scaleX 0→1` from centre (spring dampingRatio 0.8, stiffness 600).
- 120 / 230 / 340 ms: inner plates, then middle plates, then outer collars slide in from **both screen edges simultaneously** (spring dampingRatio 0.45, stiffness 500, with a visible overshoot and bounce). On each landing: whole symbol `scaleY 1→0.97→1` + light haptic.
- 600 ms: wordmark fades in and rises 12 dp. Total ≤ 1 s, tap to skip, reduced motion = 150 ms fade.

Draw a **full storyboard for MOT-01** (6–8 frames, matching the attached key frames) and add these items to the motion catalog with their own storyboards:

| ID | Moment |
|---|---|
| MOT-25 | **Branded loader**: plates load pair by pair in a loop. Determinate variant on "Restoring your history" (each synced page locks one plate pair). Add it to the component library as `RepzLoader` (indeterminate + determinate, sizes 24 / 48 / 96 dp) and use it on A5 Restoring history and in pull-to-refresh |
| MOT-26 | **Start workout press**: the icon-sized dumbbell inside the hero button loads its plates (~400 ms) while the container transform (MOT-03) runs |
| MOT-27 | **PR plates**: in the PR celebration (MOT-07/08) a small dumbbell's plates slam in recoloured to record gold with a stronger bounce, then particles burst from the plate edges |
| MOT-28 | **Workout finished lift**: on the summary header the loaded dumbbell does a short lift (−8 dp, settle) with a success haptic |
| MOT-29 | **Empty-state idle**: one outer collar wobbles every few seconds (max 3 loops, then rest) |

Update MOT-17 (sync indicator) so the "syncing" state uses the plates (pulse outward one by one) instead of a generic shape.

## 5. Deliverables & answer format

1. The updated canvas (all boards), including a new **Logo** board (§2) and the **MOT-01, MOT-25 to MOT-29 storyboards**.
2. At the top of your answer: a **changelog** (board → what changed) and a list of **anything you could not do or had to assume**.
3. The updated **token table** (Markdown) in the handoff board.
4. Any **questions** about the logo go in a separate list at the end. Do not apply them.
