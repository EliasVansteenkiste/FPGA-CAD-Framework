import subprocess
import os

def placer(options, placer_options):

    # Basic command
    command = [
        'java',
        '-cp',
        'bin:dependencies/args4j-2.32.jar',
        'cli.CLI'
    ]

    # Add optional arguments
    for argument, value in options.items():
        command += [argument, value]

    for argument, value in placer_options.items():
        command.append(argument + '=' + value)

    # Call the placer
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    #p = subprocess.Popen(command)

    # Return output
    out, err = p.communicate()
    return out.decode('utf-8'), err.decode('utf-8')
