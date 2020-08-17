<div align="center">
    <h1>
        <br>
        <a href="#">
            <img src="https://gitlab.com/technowolf/nyx/-/raw/develop/images/nyx-logo.png" 
            alt="Nyx Logo" width="200"></a>
        <br>
        Nyx
        <br>
    </h1>
    <h4 align="center">Do you have secret to share? Hide it in image with secret password with Nyx.</h4>
</div>

<div align="center">
    <a href="https://gitlab.com/technowolf/nyx/-/blob/develop/LICENSE" target="_blank">
        <img src="https://img.shields.io/badge/license-MIT-brightgreen.svg">
    </a>
    <a href="https://gitlab.com/technowolf/nyx/-/pipelines">
        <img alt="pipeline status" src="https://gitlab.com/technowolf/nyx/badges/develop/pipeline.svg" />
    </a>
    <a href="https://saythanks.io/to/daksh7011" target="_blank">
        <img src="https://img.shields.io/badge/SayThanks.io-%E2%98%BC-1EAEDB.svg">
    </a>
    <a href="https://www.paypal.me/daksh7011" target="_blank">
        <img src="https://img.shields.io/badge/$-donate-ff69b4.svg?maxAge=2592000&amp;style=flat">
    </a>
    <br>
    <br>
</div>

# Overview

Nyx lets you hide your secrets in images with secret password, Only your friends, family or a person
having the secret password can unlock the secret message. Rest of the world would just see the
beautiful image.

Nyx uses concept of steganography to hide the messages in images. Since processed image will be
identical to original image, No one can really know that there is a hidden message in.

Features: 
* Hide Your Messages: While images are visible to world, Nyx harnesses the pixels in the image and
modifies them in such way that even trained eyes would not be able to recognize the difference.

* Tons of Photos: Don't have photos on your device? Don't worry. Nyx uses Unsplash API to provide
you with tons of photos to select from. However, you can always select images from your device.

* Invisible Changes: The algorithm Nyx uses, makes the changes to the image totally invisible.
There will be no image distortion.

* Multiple Layers of Security: We have added multiple layers to make sure your secrets remains
secrets. Nyx encrypts your message with secure AES-256 algorithm and then write the encrypted
gibberish message to the image.

* Hardened Algorithm: Nyx uses LSB(Least Significant Bit) algorithm to hide the messages in image
files. Trying to break LSB is significantly hard since it is very hard to differentiate encoded bits
of image matrix.  Also, Nyx spreads the encoding of bits throughout the image selected so it is
even harder to determine which bit was changed. Even after attacker finds out the message from
processed image, message is encrypted with AES-256. And without password the encrypted message
is just some gibberish.

* Share Images: Nyx lets you share the images you have processed with secret message with world.
You can share the images to any Social Media, File Sharing Tools, or directly through
Bluetooth and NFC. Secrets are still recoverable on the other side.

* Resistant to Attacks: We have tested the processed images by launching several attacks.
We used stegdetect and a modified version of stegdetect. In most of the test cases, detection
was negative in processed images. Though in some cases we managed to detect the encrypted message
in images but since it needs password to decrypt the message it was useless.

* Designed with Material Design: Nyx is designed with concepts of Material Design to provide
intuitive and best possible user experience. Also, Nyx supports dark mode throughout every screen.

* Ad-Free: Nyx is being developed for the open-source community and for the people. We don't want
money, We just want people to use secure medium to convey their secrets from prying eyes.

Disclaimer:
Nyx is being developed by TechnoWolf. Nyx is designed to provide an intuitive platform to enhance
the security and anonymity of its users. The protocols and Algorithms used to develop Nyx are
largely considered as state of the art in security technology. Although Nyx will be constantly
updated to match the best practices available in current security technology, and eliminate bugs,
We do not guarantee by any means that Nyx or technology used in it are 100% foolproof and unbreakable.
To achieve maximum security and anonymity one should utilize the best practices available to keep
themselves safe. Please do note that Nyx is still experimental project and it should NOT be used
for real world deployments. Nyx is provided under terms of MIT license.

# Contribution Guide
Please take a look at the [contributing](CONTRIBUTING.md) guidelines if you're interested in helping by any means.

Contribution to this project is not limited to coding help, You can suggest a feature, help with docs, UI design 
ideas or even some typos. You are just an issue away. Don't hesitate to create an issue.

# Emailware

Nyx is an emailware. Which means, if you liked using this app or has helped you in anyway, I'd like you send me an email
on [daksh@technowolf.in](mailto:daksh@technowolf.in) about anything you'd want to say about this software.
I'd really appreciate it! Plus I would be more than happy to know my initiative helped someone. :)

# License

[MIT License](LICENSE) Copyright (c) 2020 TechnoWolf FOSS

Nyx is provided under terms of MIT license.

# Links

[Issue Tracker](https://gitlab.com/technowolf/nyx/issues)
