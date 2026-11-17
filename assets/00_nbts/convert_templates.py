"""
Converts the finished builds in this folder into floor plan templates for the almanac.
Every block matched by a rule in template_mappings.json becomes its role block, everything else becomes air.
Usage: python convert_templates.py [build name ...]   (needs: pip install nbtlib)
"""
import fnmatch
import json
import os
import sys

import nbtlib
from nbtlib import Compound, Int, List, String

MOD_ID = 'hbs_builderalmanac'
DATA_VERSION = 3465

# Role blocks, the first four match BuildRole.java
ROLES = {
    'primary': 'minecraft:cobblestone',
    'secondary': 'minecraft:oak_planks',
    'slab': 'minecraft:cobblestone_slab',
    'wall': 'minecraft:cobblestone_wall',
    'stairs': 'minecraft:stone_brick_stairs',
}
SLOT_ROLES = [('primary', 'block'), ('secondary', 'block'), ('slab', 'slab'), ('wall', 'wall')]

HERE = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(HERE, '..', '..', 'common', 'src', 'main', 'resources', 'data', MOD_ID)
METADATA_FILE = os.path.join(DATA_DIR, 'almanac_templates.json')


def full_name(name):
    return name if ':' in name else 'minecraft:' + name


# Rule pattern is "block" or "block@minY-maxY" with either bound optional
def parse_rule(pattern, target):
    lo, hi = None, None
    if '@' in pattern:
        pattern, span = pattern.split('@')
        lo, hi = [int(v) if v else None for v in span.split('-')]
    role, _, variant = target.partition(':')
    return full_name(pattern), lo, hi, role, variant


def find_rule(rules, name, y):
    for pattern, lo, hi, role, variant in rules:
        if not fnmatch.fnmatchcase(name, pattern): continue
        if lo is not None and y < lo: continue
        if hi is not None and y > hi: continue
        return role, variant
    return None


def to_state(role, variant, props):
    if role == 'double_slab':
        role, variant = 'slab', 'double'
    name = ROLES[role]
    if role == 'slab':
        slab_type = variant or props.get('type', 'bottom')
        return name, {'type': slab_type, 'waterlogged': 'false'}
    if role == 'stairs':
        return name, {
            'facing': props.get('facing', 'north'),
            'half': props.get('half', 'bottom'),
            'shape': props.get('shape', 'straight'),
            'waterlogged': 'false',
        }
    if role == 'wall':
        state = {side: props.get(side, 'none') for side in ('east', 'north', 'south', 'west')}
        state['up'] = props.get('up', 'true')
        state['waterlogged'] = 'false'
        return name, state
    return name, {}


def convert(key, config):
    source = nbtlib.load(os.path.join(HERE, config['source']))
    rules = [parse_rule(p, t) for p, t in config['rules']]

    kept = []
    for block in source['blocks']:
        entry = source['palette'][int(block['state'])]
        x, y, z = [int(v) for v in block['pos']]
        rule = find_rule(rules, str(entry['Name']), y)
        if rule is None: continue
        props = {k: str(v) for k, v in entry.get('Properties', {}).items()}
        kept.append(((x, y, z), rule[0], to_state(rule[0], rule[1], props)))

    if not kept:
        print(key + ': no blocks matched, skipped')
        return None

    mins = [min(pos[i] for pos, _, _ in kept) for i in range(3)]
    maxs = [max(pos[i] for pos, _, _ in kept) for i in range(3)]
    size = [maxs[i] - mins[i] + 1 for i in range(3)]

    palette, palette_index, blocks, used = [], {}, [], set()
    for pos, role, (name, props) in sorted(kept, key=lambda k: (k[0][1], k[0][2], k[0][0])):
        state_key = (name, tuple(sorted(props.items())))
        if state_key not in palette_index:
            palette_index[state_key] = len(palette)
            entry = Compound({'Name': String(name)})
            if props: entry['Properties'] = Compound({k: String(v) for k, v in props.items()})
            palette.append(entry)
        used.add('slab' if role == 'double_slab' else role)
        blocks.append(Compound({
            'pos': List[Int]([Int(pos[i] - mins[i]) for i in range(3)]),
            'state': Int(palette_index[state_key]),
        }))

    structure = nbtlib.File({
        'size': List[Int]([Int(v) for v in size]),
        'entities': List[Compound]([]),
        'blocks': List[Compound](blocks),
        'palette': List[Compound](palette),
        'DataVersion': Int(DATA_VERSION),
    }, gzipped=True)

    out_dir = os.path.join(DATA_DIR, 'structures', 'almanac', config['category'])
    os.makedirs(out_dir, exist_ok=True)
    structure.save(os.path.join(out_dir, key + '.nbt'))
    print('%s: %d of %d blocks kept, size %s' % (key, len(blocks), len(source['blocks']), size))

    return {
        'id': '%s:almanac/%s/%s' % (MOD_ID, config['category'], key),
        'size': size,
        'blocks': len(blocks),
        'slots': [slot if role in used else None for role, slot in SLOT_ROLES],
    }


def main():
    with open(os.path.join(HERE, 'template_mappings.json')) as f:
        mappings = json.load(f)
    with open(METADATA_FILE) as f:
        metadata = json.load(f)

    for key in (sys.argv[1:] or mappings):
        template = convert(key, mappings[key])
        if template: metadata[key] = template

    with open(METADATA_FILE, 'w', newline='\n') as f:
        json.dump(metadata, f, indent=2)
        f.write('\n')


if __name__ == '__main__':
    main()
