from setuptools import setup

setup(name='pebble-remote',
      version='2.0',
      description='Remote control for LibreOffice Impress with Pebble',
      url='https://github.com/LibreOffice/impress_remote/tree/master/pebble',
      author='Gulsah KOSE',
      author_email='gulsah.1004@gmail.com',
      license='MPLv2',
      packages=['pebble'],
      scripts=[
          'pebble/p.py',
          'pebble/data/scripts/pebble-remote',
          'pebble/data/scripts/exit_click.sh'
      ],
      package_data={
          'data/i18n/ca/LC_MESSAGES': ['pebble-remote.mo'],
          'data/i18n/tr/LC_MESSAGES': ['pebble-remote.mo'],
          'data/i18n/es/LC_MESSAGES': ['pebble-remote.mo'],
          'data/i18n/it/LC_MESSAGES': ['pebble-remote.mo'],
          'data/mimetype': ['pebble-remote','pebble-remote.png','pebble-remote.desktop']
      },
      data_files=[
          ('/opt/pebble/i18n/ca/LC_MESSAGES', ['pebble/data/i18n/ca/LC_MESSAGES/pebble-remote.mo']),
          ('/opt/pebble/i18n/tr/LC_MESSAGES', ['pebble/data/i18n/tr/LC_MESSAGES/pebble-remote.mo']),
          ('/opt/pebble/i18n/es/LC_MESSAGES', ['pebble/data/i18n/es/LC_MESSAGES/pebble-remote.mo']),
          ('/opt/pebble/i18n/it/LC_MESSAGES', ['pebble/data/i18n/it/LC_MESSAGES/pebble-remote.mo']),
          ('/usr/share/man/man1', ['pebble/data/man/pebble-remote.1.gz']),
          ('/usr/share/applications', ['pebble/data/mimetype/pebble-remote.desktop']),
          ('/usr/share/menu', ['pebble/data/mimetype/pebble-remote']),
          ('/usr/share/pixmaps', ['pebble/data/mimetype/pebble-remote.png'])
      ],
      install_requires = ['setuptools'],
      zip_safe=False)
