import subprocess
import os

def placer(options):

    # Basic command
    command = [
        'java',
        '-cp',
        'bin:dependencies/args4j-2.32.jar',
        'cli.CLI',
        options['circuit']
    ]

    # Add optional arguments
    for argument in ['architecture', 'input', 'output', 'placer']:
        if argument in options:
            command += ['--' + argument, options[argument]]

    # Call the placer
    os.chdir('..')
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    # Return output
    out, err = p.communicate()
    return out.decode('utf-8'), err.decode('utf-8')
