package de.jarexception.advancedsoundaddon.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

/** Selects Engine Sim exhaust transfer kernels by engine family. */
final class EngineSimImpulseResponses {
    private static final int SOURCE_SAMPLE_RATE = 44_100;
    private static final double STANDARD_VOLUME = 0.01;
    private static final double DEFAULT_VOLUME = 0.001;

    private static final String SMOOTH_39_PCM_ZLIB_BASE64 =
            "eNq1ewVUVV279Vo7ToABFipid2B3YGGBgYGFiB0o+trdhd2trx2Y2KKiYHeCIAZhYmDAqb33WneeLe97v3vv+L/7fWPcfzxjn3PYZ++111rPnM+cD+IsOlZ4" +
            "JlQX+4iDxACxlPhQCBUkYSetT5PJJhJEjOQ0n8Jb8mJc4IlsO+vDarAizJ3lZsVYJVaNtWTBbDSbyOaxnewsu8xi2DUWi/dodo4dZ/vYVraBbWJr2Aq2iv2J" +
            "M2fZabaNrcTPy9hSNgv37WZX2Qtm4PV4Tz6XX+Aar0jaklFkI7lJHKQC9aENqTeiNt5r0yq0Aq2J9xqI+rQFrUvL0TK0Fu1MA/FNTnzuRMfRcLqabqd/0l10" +
            "I51K21ITTSAxJJpcIPvJEjIFx3oyh/QgJcl7fpxv53v5Lr6eL+cz+Eg+gk/j8/hUPpz34K15XV6OF+QFeBFemTfET1V5Be7J3XlxzNcfV6/lh/hF/p7nJVWI" +
            "LxlCFmLkDWQGCSBNiB/pjHNliUxe8XN4wko+nY/lo/hMvobv5hF8K1/Gt/EbPIMbSCFSlHiR0qQO8SYFCee3+GbsRg/uxU14WhFekTfizXkr3pcv4Dv5SXwf" +
            "z+/zKH6Eb+Lh/A/ehVfiRm5Gnnx5b8z+ME/g7qQjCSMrsfI3JJOI2J3cVKIqeUeukO1Y/0DSitTDPP1JS9KItCEjyWZyD3tehNajQXQOXYvYQlfQ0bQH9vY1" +
            "WUt6YscU/hZPP88jMcMgzO4WcjyR+bP8TGaqZtMKsQA2m+1iz5knD8Z1ObCm8SSWUNqIhtH5dBYdQbsidyWoEWOexTOnkRBkvA3mkof84h/4F8QNvo5P5POx" +
            "uwncjdTGtx1IFxKMnQ0nw0gtYiA27kkak2qYURWsoTHWuoEkkly0OjBSmjag/ehkOoa2pq70FTlFZmN1ozB/T/KBp3AZ9wUi/zVIcdKQTCRbyFayHO/99LyZ" +
            "yU2+A9mZgR3/zuYwb/ZLe6Jd0Y5oczUfzaaeU0eq1dUc6mtlreKrFFAMSnHFT2miiMo9R7TjriPd4ar4KzuURKWsOkdNUftrTFvHCB/HzWQPaULf05HCY6GD" +
            "KEpnpUbyDjlDzmOghptyqJxXvigNlD6JTcXHwmUhSlgldBMaIhoITYSuwmghQnAIBcXiYgWxvThMHCL2FAPxOlccJ3ZG1BRzih+FNUIRIY5epkvAkDdkH5lM" +
            "apK32Mmq/DW4OBqr+ayd17Zr47TO2ghthhas1dckLU6NUz+qN9Ulage1vtpA7aFuQvyhDlQj1HxagDZA66QV0r6prtiBEdoYravWRKuOTxe1BC1Nu4F4r5Vh" +
            "i1AFLrKywKgrOU/86WYaR4sJVYWOwgGhkDhHfCmWlXZJAbJdnmmQjVONScayJj/TLFOUab+pu4mYLhsXGqsalxpS5b5ymhQiXRczsfrDlNAGJIjvZ+1YPtZJ" +
            "663uV4zKXEd9R4Z9ib2/fax9lj3A/tQ2wpbPdtgaaBWs8ZYNlhKWo1mDs8pn5ckakJWRNdtS2hplrW7zt02xCfb1dldHB8dDxzJlhDpa68t+sHzgzzgex4PI" +
            "L5ID2OxAA+hiWki4K8wVr4i7xINiEWmSdEX6IkVKU6SaUgEpn3RNHCFahMGCl3AZaMtPX5LPpDntiWhBK9LZtIpwW/AVn4oRUoRcwvBIbiLPkUaJb4SXyKOn" +
            "0JVORG018JPsLssLpk/iXbmKyjibXWF1kK1CqLtPgJhSJJ7X4GXBhHwkH1nDz7MkrYfWXktQg9VPygylo9JMuaRMVd+o7bTbmifriaoayUpzH/6ANWAbtEra" +
            "DXWj6qfuVhorRZUtyhY1RNujNWCcXeRlSDlUntJgQwNSBpUnlVQVGovHRFfsfSepvbRZipKWSy5SfrGikEBP06V0K82g/sIWQRLfiIlSjDzNsM/gbhRMBvMR" +
            "8xRzmqmf6bWxlTHRUMzgkHaLBwRvYTDdTJ7xEay01kqdoCx2nLXvtL2y9rHesbyzlLNGW2ZZeluGWNysG60lreGWCpbhWdbM3VmJlkVWb5toH2hfZJ9ub2Iv" +
            "ay9sf2vLtLWxN7WH2vfbE+zbHGsUT7WW2lM9pObSvmm7+Do6UrwlRcubDJuN80w9zS1cSrpedi2do0WOy65NXV+69HR5ZV5o7otZ+poOGocZOxnzGO2GKYZT" +
            "sptsEwcI+eh41O89JC9pz1eyg9oR9ZzSSFmgbFDClXlKqLJVCVQHam6sLOvAfrE9PJA8I6WpB3Uj+dk7JdnexNbS+s3yOKte5vOfDX48zpiY4fZ94ff23/N/" +
            "l76fzaiSIWVEZrz7fu3n9syuFl/bQsdotRdrQZ7QlmKS1MUw0phkGu7S0HWAawfXLNcOOQfn8sldym2Hm6f7hjzj8nUscMYjziPcY6THZg+rR42CDQvmK7jC" +
            "o7fHk8ILSx6oeKp2qRZ3/UZ0HtXHc0iHsBJTITH/JFqOo8FLmkSWeldkiMcyo2/W6MSIk5O2xmxsvKHYqnmLvyxauu5aZPU7wU8+xoek73f5Wd61QdvGIc2q" +
            "tlPa5fVfHNCwW0Lwi6HRQSN8aLnBXms9P7useBt8PerSngsp0XVvVbrW+EyXqGdPj37vRycYYsU79jVfg9LaxPe7XjgqNLLajuKLyaz0OUNW5d4dfLnn690O" +
            "//yLa/QK+efzNYXubTo27+qUobEPT0TuGbX79eF2J0LOed7fkDrsXXzyzKTO99/cSHxQ717Zi++iQq5euBR6qvqpSxF9VpNZ/1eRsG3vnXoG927/fJ7/NU77" +
            "fcy5+0HBnebZ/320EpsXXm716WORvO03hf2/7u46+fd76Jj2/yWb6YOfNFtXqEb6kZjC+/cs/1fmPm8d33x7o/q/Xttw9Yq71WiDMoU7/iurOxX2ZKjr5P/t" +
            "qqL9QkqUeGs/3+v/MBf/PEwbTkfvSaz3KfeX8a/SY08c+Lja9o97sYPciv3+qfChNj2n/zu5/Fdi0YTV/Zd17Nr4fGU3rwE5h7F8P5S3xV/4Psp10yd61eka" +
            "R6L3btwevGHmSpfFn+b+8zUUmNVzXvgi82rL3hnnN974/sg/6fnbqZ8rfq+QJTg8+UWhtLBdK2UZ87O5uta9kHfjLv/uTO9OqNq+eqUKOd/+OJT46nbSDe3u" +
            "4MfLnt6O7/06/f2kb1N/rbKE2XNkWd99f5bzqvnY6+0jVv57OXiw4PC25ydvX898Wi654Oewn9asROtj6yhLeKYjY9QHnzdHks4ksaTmScEvfVIef6j47fKP" +
            "5J/MovE3pmm5wwvcKbayckijTu2Hh7Qf96+v6dzoMUGfWneuVbFkAXd/dvKj8enxaPlQ0vqp/59w5rrAc1Xylsv7l51Yd2H+dcejH0kTP86wDpO3uQ8qmlS5" +
            "bKNp7bYFtghxG7biD7dJ8VPjpt/6tzIUPH7Z8PTgNl3v+89v/at5VpNhDey1JlXtXvFHmd0lNha5UKCm+zjX3tJKtWHmq/TI5Lbxbe7vuhEc0/hSYNTBc65R" +
            "l6+0vhuWcP9DiG2YeUTh2xW9G8xss65bk35DQ/ONq/s/tKHwzNpTTow7GFZqaNt+NXv97HzR71DLjY0X1jlUNXeFEyWXFllR4IFbXdcU+SaxK/2tRX75Zpz+" +
            "PPDjxHfvUpcm938d8nLlCznx1PP18cvj1j77/nTX02VPzz9VnpZ4FvhszzMxbkNcj3glfv1z3wSXxLjEiS/yJu1N8n75+mXEqxGvK71hb5KSF6VUSo1PHZH2" +
            "K23h24rvnr2b9b7Kh8QPUz9W+SSlf0tP/Wz9Uu7bsIx93xN/uPyqntk2q5eln7WtrZRdcHxwPFXuqs81KytEfKifUEpMFrdIPvJLeYxBMYwzphn7mJJMPuYl" +
            "5svm1+av5h9mi/md+Z75Md79XVa5bHMd5Fa1oF+J1pVInT1NzX7junXo+2OIcdSccd6TCkyZPGXl5DUTfcZnjBk7+ufIHqELhh4adLx/pb55etsCfbuEd6zj" +
            "92erxGY3Gw2re6T64EobS6/x2lBgUm5v0w9y3t7059r0RmnVX1aOG//A51aj2JUXJ531ONnpWPnDxSN8D3odnHNwa8SIw7eOzolccerN2bIXVkcHxea80e/2" +
            "untLHzoeez6bHP86IeNF0stFr++8+Zjskvo1dUja7rSotMNpf6RVTyueZkw7lRqYmphSO2V/8oLkL8nBKVdSVqZeSxPfdXwf8yHoU9HPxb4OzLj2o0LmfEuq" +
            "zUsprw3nx+gcMV3i8glDUeN6o2yqbWpqKmSKNHYxFjZeN7gbKsnDpZtiQXGskEHXoIfuT7vRudQkJAopYoZ0Xx5leGYQjdwQYahseC/flyPkwfJ3yVfyETn9" +
            "iZ61E7/CfNgR9AduWk4tv9ZA26/VYYdYVfSAOdFVdSQV4GQD+XP406Jsh9ZCy6WpagHNX4vQOrIyPD8x0HSaJNwRj0or5P4GyRiMbuCw8aRxrXG0sbuxl3Eg" +
            "zvQwNje6GpMMFw3zDS0MOQyH5HzyIDjNF3Cam9HdTiMCWcZL81OsEXuujdSIdlZdq65Rd6tX1C9qXq2xNl9L0Zqxg0yE46+M7p7yKNaXubD1uHakqsDlVVDO" +
            "Ovo58jru25fb69rTbS9sX2zu9j72F/ZZjmZKAdWuvtBusli+m3Sn3+g8wVt8LU6UbJK/vF7+IQcYzhs8jf2N542lTMtMP00tzTPNe817zIfNp4HEduac5tum" +
            "Gaaqpifof+rDgXJDFjLSy7jSmGxsYhpsCjYNMi02bTWtN20xnUJndM2U0xxsXmA+Z1bNRVxyuhhc0s2TzVmmKab7xreGd3K8FCHOFNrRH/CsvpqXWkUZAI/d" +
            "1P7L9tlWw77F3sBRXqmi1tIasRz8AK+GLvk7aYW+/ivNI1joHbqdhtKaNDctSFsi34nUG31mrFBZXAo21ZZWSd+klvJW+adcw9DdEGhoZahj4PIpeb7cQbaj" +
            "fz0hxUrXpDtSIuK+FCPdkx5Ia6V+0iCpt1RSeitGioNFk3hTWCvMECYJI4ShQhehrlBU+Eqf0li6kPrRCtRBjpE+xEgu8OU8hLfknXhNnsJWsyrsuDZIa6mZ" +
            "tMNqkGpUdyn9lcHKKGUMeut4Jbc6GD1roJao+bFdzIIOLg9vyKfyI/wG/8QrkvHkDLlB5pCS5BVfgzyvZAmaWfNWOyl/ODbb42xlbIutonWjpYVFsqRlJWUl" +
            "Z1mySlsGWCItOayjra+sXWyPbd2Q876Ot45Q5ZsyVlXUyVqWNpqlsyB+l9cmG9ErtsFOptFSQl9hnXBd+IXuvC468eHiLHGNuEM8Kp4TL4k3xPviPfEaPp0U" +
            "I/DNT8FDKEq9SD5u0a6r85ROjsL2JOsLi5tldJYxKzpzRqZ/ZkDm2sz4zEJZi7IEy2QLR807ge71T3RAHe1n7DUdhxxuymqlgnpNHYQ5zWOl+EM+jXjTJNpH" +
            "z94O0QMZWIzcmOQe8kJ5jXxQ3iFPkEPk6vJdabxUR4oRS4urhMfUnQ4kM/hydhe7M0j9gn6nqZJTya1UVfyVpcorxVd9pDYHl80skG1h31gN3o4H8TF8APdH" +
            "x9qf1weDEsHzTWweG8Cms43oYQ+xW+w4240jht1jZ9hOthmMu8WesE+sGnI8nK/gl/g37kfukmZ0Kj1AGe0sHBfqiqfQew6REqRa8hh5Pyq5q6EaMNccNcdk" +
            "SJBPynNlHzkNHXhZ6Zk4U6wvMnTTiVhxlPBC+CT8FIqJfcUNYhxGqSd1l0KlJdJh6a5klTzlynIj2V9uJdfE0V/uI/fCbqyWt6COXZAfyM/lJNkuexoaGUJQ" +
            "WaIM7w25jE2MbY3tjH7GmmCpakg17AT+ixp+yOflULmW7C2Xl71wVJYLycVkX7mTXEMWwcYT0mSpoWSWksU74nlxsdhOLCO6ig+B/4aCINxGbR1B/WklKtKz" +
            "JAx9t4Wf5EN5Lh7PTqEydmfNWQlm155rkVo40F9Lc9ds6hHVR32pzFYKKwcd3R3FHNfsPe0u9pu2aTYfm5vth/WZ9aT1sPWm9Y3VYnW3VbUNsB2wpdg87MPt" +
            "j+xVHKsdRmWZkkudoxq0edoXzRf5cOE9eTg/xZM5JcVJI+JHepChZBE5Rx6RL8SLVqTlaBtUhln0EI2jWUB4kPAnEH5VOCbMF3wFjR6hPbGGo2BubnKDL+IB" +
            "vCS3s0R2ie0HFlazZYh1bAc7wi6y++wNy2Qm7gVmt+bBfCyfx9fxCH6RP+AfOOcexJu0JIFkOBlNfEg6n81VNoZlaKOw9mmqVQlTXjj8HFftLe0PbCE2F9td" +
            "6xxrS2sFawFrDmtRaztruDXB6m1bYEu39bLfsJdy/OGIceRR+ikRSjo4MlBdr15SX6qZqNwWNY/WSOuujdG2arHaBy0/a4Dqv5Y9Y+68FZ/Pr/B0nptUI61I" +
            "N9IBqlWD5CWp/DAfzb35e6C4Nfuh7dWGaHW0gporKpMn9C5Ym6ud1D5r5VkftghZTGZmXN2V/4F1rMG91/gb7kKak3DynJSkQ+kJKgqBwmbhsZAfaN0hPhcF" +
            "qZzkJ02Q9kovJFe5itwc+JwgLwFjo4BLYihkKIu6G2AYaQg37DVEQ4lTDe8Mrwy3DAcNkwwNDap8RQ6XA+UKsiLdlFZInSRBOi6GiLnF40IL4QWdQPPQC2Qc" +
            "0OaJbFFgTuEO/pW/5DF8O2rmYN4P+avPKyBDebmZC+A0h0K68cK8BM5W5S14GN/CH3EDsDKTXCSZpDodSffRFKhII2G4sFV4IjChghgsLhGPiI/EX6IbONpI" +
            "CpCCwcJJ0hgpSOqMijQEMRgrXS3tkyKl09JxaTs4Ok0aJnUAYytLpaScUqb4AQyOEneJ88Q+YmOxvOgmWsHyI8IsobtQSvgC5diNqtGN1kP1SiNRZDOZSPzh" +
            "Qd6i+k/gjbmCuhPOfBnTLmqztfZaPi1e3aGGqrVUs5qmnFeWQ0W6Ky2U6koxcKq4UlHxgbYsVg4pDxWbUlHtqc5Sd6lX1TRV0kpr7bQwbZV2SfuomVgp1pC1" +
            "YX6sA+vMOrG2YGszPMf52o71YgNZKBsBPPmzpqw+q8QKMhl4ua/t1mZpvbXKmqYmqifV1epUYLKdWkctpuZW3dVCajm1vtpWDVD7q1PUjepZFa5WTVa/qRoY" +
            "664V1ypoVbSaWl2tvtZaG4ix1gKDUVqc9g1VuQgrycqxiqyy/u8xNZk3dLOePqvOLIytZ1fYF1aY+/JRfA9P5DlIG7KYxJBPxERL0jq0Ne1Mg2hv2gU1qSmt" +
            "Rr1oLipTK/lAnqAWbCHzyCjSC5wsTghJ4Ef5Yj4ICl2Wy/wdqvk9jL6bTWP9sSPlmYKV7gCzWmK/U9Uz6lZ1lbpcXQn2bVePqbHQkVfqV1VV3cCeEHBmr3YX" +
            "DGSaJ6vL2mOMiWwhKsZRVIuPjGHOLTDnLWDPF+6m/+tKDxKK1ms9iSCxJBEuJgeqqD8dQ1fTU/QZtVBPwV+YJ1wRiOgrzhGviJLkAx+ySDoqPZUypVyo0uVR" +
            "t5vL7eTucj8oYU+5qxyAn5pCE4rKVE6GUsQAlWukMKm1VFhKF8+Kk8V6oiY8FPYI4zF6VaGAIAoO+p1+wqHSHEI+IS/UvJRQXagPpnUVQoR+whD4nOE4Rgr9" +
            "wfMOQkuhtlBGcBdU+o4+pFFgzRoaTmfSMNqHdqXtaGNanzahPohmyEEDuLFKtJTz94rUhRpQY414NyMrEn5yfnZFjtzh2HLgs0hVYkU248hlspcsgesZQDqR" +
            "pqhbpUl+5CwdnD2JWjuF9+GNuAfPZC9YNNvOFrBRrAeqWX1WmrkzDlWI165rp7VD2kEckahnx7Sj2hmgLBLav1PbhzNXtSdwW6+1dM2h5QYT6oEFIWw0srYT" +
            "Gn+PfWASNMCH9+Aj+UK+j19GdX/M7/N7eH0IV3aJR/JtqLHDeXfeHO67BC/Ki/BSvDbQGcB74y7nv22dwZVPeQJ/wZNQn57zWH4QVXQy7wvlqIIVyPwbew5N" +
            "2cCGsjqY+RVoWkdw5Kt6AcwaA6fYXm2uNlabqR3VvvCIo9UZ6kJ1CbC4Sd2jHlXPqdHoBWKAx+vqHfWpjklR89JqoxPpC6ZP1RZr67TtWPFB7MBpjH9He6a9" +
            "1N5qPzRVk5kbK8C8wDcf1pEFge/T2Rp2APN5AtSKvCLq/gTMN5I/4z+4GVloQrpA5xeT7XC5MVDXeGA3njwk18l5cpjsIKtQT8NIP+hNW9KM1CeVcY8XcScM" +
            "/igF+3YdmnQeSn2Sn0O1vo89+cw1ngfdVDMwIkz/jXU0lOUr0cCIIvDT9aHcPeEyZtBldDONoBfofZpMM6lZKCbUEzoLw4TZwgZU08vCfSFJ+CzYULmpKImi" +
            "qAjf4KfuCGeg9UuEyUByJ7iWikC4IGShO3tDn4NpD+gNepVeoWfoYbqTrsBzJqFfDAWau9GOqChOJDehtWhpVJPccPcfMLvr5Cw5jvVGkAOIQ+QkuUTu4Hwa" +
            "PLSI3qMqbU4D6RCweSZmvYnupZGY9zV6k96jj9CVvKW/KBVyC55CWbDJF/PqKQQLAzDDIcJAsC4Y/qSnECC0wgorob/IKxjB0y/ww/dpDOpDBN0B1i2AEg6m" +
            "3dF1NKGVMbsc1EbeksfkKmrdQfj5BdBIZyZawgEUg1IyuJM7qHqboOQDuR+vzvPxLJbEYlkEW4561Q8KUAsMysMEZgGHUrUE7bF2W7sGrsRo0dCMy8DPJajQ" +
            "Be0ssHQW71e0m9oj8CgNPPqhEZaTFQKa6kJDOkBBBsP3TIeLWMf2wUnEskfsFTzzT+ZgnGlMwbvzNQthZyrCob+qOGeHw8rAtW9YHLvLrgGRJzDGNnib5WDo" +
            "HDaDTWWT2GQ2hY2DLgyFVg1CDEDd7Q/F6g0s9wWbQ1gwPgXjc3/9+4G4YjAbhhjJJsCfhsPfR7Bz8PLPWQqe9oNZUasl7grf4IUuqzaqth/vAncZBJ8/AL4i" +
            "CPUggLdFXajHa8BJVIafKMfLIIqD/Z6IAtjV/IgCcBtF9PBCeOKnQjjnDOd3hXgxXp7X4s3Qd3TDqP35EFSMsXw8+DYWMQ5+K4yP4KFw084YghiGn8PQq0zm" +
            "c9FxbIMXu8zjoSeUeEBRmpPuZASZTdaRffAQD4FFC3GhnvC+DaAr/YCVZajVV4C+n9RFKC7UEtoIvdEdzxKWw73tF04KMWBQvPBG+ChkCFlgkSyaRRcxp5gD" +
            "7waRQjls6Am/CKngWZxwV4gWjoJZy9ALjAZmA6AM9QVvqEdhqIMR+pCJrvwDPNVzcOwavQTcHsUMdoDFq+kiOp2OpgNpL3CsFW0IxpSlRWkBmhPKwODHMshn" +
            "6MAH4DmVJJOU7OMNjlSsLAVHGj6l4HhNXuL8a5JEXpAEVKMkRKL+/gKMfIqduIPO+Qo4cRw7s4UsR4VyuoC2pB4pTwqTnITz7zwN9fkp2HENdf0c6t0R+PkD" +
            "fD/q/h49dkG71/GVcAxz+XRkaQQYFAwsdEF/3wF48OVN4ThrAA9lkPE83AWuU0Uf/xMo/grP8gW9ZgZ+sgHxnMn4PhcwVhCo8IJuFEMUxef83B3YkzgDI76w" +
            "NHDzKbvDLrPT6Dt2oV9dDS7NBO7HAvPD2RCgORgs6wq/0pI1ZrVZVXCvDCvOPJkHWJyLuTBXHGZmZCIjTNOs6K6/g9np2icc37RfmkVTNAFXuIO3xeF6qkFD" +
            "W8K/dAdjQsGtadDWVVDYwyyK3YAqvGGfsSrCjXDTHphzaShfDV4XLrUlOulOPBDK1x94HcUnocosBE438D+xkyf4BeztXezxS/4eXj0Tjp1zkRiImbgQV2Qh" +
            "J97NxKQfzjO5ULHcST5SAMrvjLzEDWdciQwf4MD9GfwTRkpDfECX85X/wogC7nTX/w24KrSnBVx0IAlCDRwKrzUabmIymQbPtYAsJSvhvDaTnXAZEcDFGfQA" +
            "seQ2sBIH3KSQ99CfLCiQDG9SgBZD91iF1oA/b0J9aQdU9mAgdzg6hj/oWDqeToRiTKST6RS4+Kl0Gmr+LDqXzqcL6WK6lC6nq4D3tXS9HhvpBj026rGZboE6" +
            "bMK783B+txbXrsEdq3DfUtwfjio/D6PNxpjTMfY0/RnOZzmP6Tg7B9/Px1UL9ZiPa+dgBtPw7Xjoz0goRAg0ogNY1ghrKA+W5YdSyHBaWeiNU8GUR+BHNDkF" +
            "HdtJNoAf88kUMha1ZABYEkBawy/XIhXhmfNjdxn2+RN/DT9wD17mPNRkP3qu9dnMmMmnIe/O2jUSGHAypBeqWwBvz9ugkjZGzawGT1EKmC8AjuQCC4xAO0e9" +
            "t7LvLJ29Z6lA2Ev03XHA2gO4sHuo/nfxeh8/PQIb4hDxf8d/fn7OEnBXEu59yV6jZ00Bez7Aw6QDr1/AwAzU9p/QlCwcv/DpO85+xvdvce0ruMgEjPEMz3zM" +
            "HuJZd6AIN/S//fodV+AyL0AnzsEZnoISHWfHwMjD7BBej8DjH8OZSLD0jH7NeXDlEu6IwZ3XMc4tjPgE47/A2lKxxnTUgp9YscoEbtJ55AUeVeY1UUGaYqc6" +
            "wnX1gMPth+5kCPyls+qPRs2ZxGdgjxfDj21BRTrCT/NofhNuNJGnwkVlwUfJYFF+UgT4rwjNr0MaQ/vbofPvikyGkIFkOCrfOGR3BjqhcJ0DW8lueIVjyP4F" +
            "+IY7qJcvgImPwP8vYiMMbsYM15MXnqYIeFAK+KmMvqomrQtVaQKX40vb6n9d0gWs6InOKxhK04/2xXsQant3dAQBqPD+iLbwca31oxXC+amtHq0RvhipKTqH" +
            "ukBoZTyjFJ7liZ4hD/TA2SsI0AQ78PodiHVqwhvU/ARw9Ql5QO6BtzfINcw+FhGDSn8ZaL6I9ZxHzT9LTmNtJxDHyJF/iKN6HNOPSP37U7j2LO6Jwp0Xsu8+" +
            "o9/tvP847jmEndpHdsHlboWObAJX1sLvLocbngdNcXImjAwDa/qSnnDJHbDzvuhdGpDaqEaVSFlSihRFV58PdeyvKvYLeXsHNiXwJ7r2RKNfOI4OYTffyjfy" +
            "1aiei9FhzES/Mw4YGA5G9YUX6QLP4MuboOo62VQS6PGA53ADn3KAUSZuAKdETqAiDGrjdFV2hA3Yd6pR+t+oTwQmnwDvd4HQ6zrKLwO3l+C1LmTHRaD4sv53" +
            "kDHZTIjBVU42ROtXXtLfnVdc0cN51VXEdXYTHLoHxj7DU16Di5/Awd/qJ3EzZpoP2ueFnqks9KMafFYDIL8lsN8e1SIQVSNYV5GRcDsTsPoZupYs4cuB/fVA" +
            "/3bs0H5+CNXnBFhwhkdBtS+jn4hFf3GD38Je3kO39hDd4mPs7FMeh84rAZEI9XnJk6EZ71HFvqKnseg6JECHXKEueZEfT2SpJLJVCVmrCY/gQ1qR9nDRweDP" +
            "CGjINDIXGV8B7mxCF7QTeNiHHuAg0HFYx9UxYCVSx9Qp4CcKWIwlt8h9YDUBLuU9fM1P9Li/mZULzPIAs4rTkrQMep1K1BvsqoFuoy48kbOD9gU72oI9HcCi" +
            "ANoZPOuMCKCdEB1oe3j/djqnWuj8aYRuqQ6iJkbxxmgV4KpKouJ7gr/5wGMXdN2EKjqTPoNHyWB7HDqGe+QmZnkJiD+N+R8i+1EVtkMf15PVwHc4FGE2mU4m" +
            "AeGjoKRDSH/SBxjvit7cifJW8J5NgPO6QHp17Fpl+KoypATqUEGSBxXJSChRoNjfsOep8FnxyMtdVK6r6AUvQUPOoh88Adwfg6M9qDuuXXwHcrwFncp6uK41" +
            "UJflfCnyH45YpB8L0V0vwOtC/LQY3y3FFSvBmLWI9fAcG3HvJr75H2JTdmzA985RneM6Y7UeK8G1ZRglHCPOB9qmA3UT9b9vDdWVrB8Q6fT+gbrjaw+v0wYc" +
            "bA7UNuENoWt1UL2rox+oBFdfSv9dQGGdlXl0VprR5/9mpAIOWMDE3xr0A0eG/v4T56x6X0RxrREuMDfu9UCf4ImximPMMhjZ+W98VfGcWnhiA+hpU8yhLWbT" +
            "GfMKQm0YiLmGYdaTMP/Z2J9wrGgFdmQD9vJPnTMR2OWTYMwF7P11ZOERuPGCv+FvkRsnGxR0EzK44AbfVRiaXxqdeRVSA5mtjxw3hZ600RWlE+kMTnQHCvpA" +
            "WQaQQUDFcPBjFDAynkzQNWY26uJCoGcJWQa+rASW1qJmbkTt3IYauhOd/j64ryPgyykg7yLqdgw661tA40NgMg7uPYm80j1/iu74U/8OZy/wGt+90D1+HFTr" +
            "MZTgPvTrNu6/iVF+q4FTCy7pWhClV/e/6rmzojs5elxXhd9K8Ju3JxGncNV/VwLnvSd1HTgKnjuVYD+8415dDXaAK9ugCZuxNqcmrMFaV2DVTt44/zhmOnZj" +
            "AnT3D509Q7FXIagmveBNu5COxA8OqwWUuj5cllMnyqD+FIaKu5Ec2cyx8Z86d96icr2A83qI+nYD+buAPJ6ADzgAxvwJhK/PVowF2U5sMhA8DhV0NFDs/Evw" +
            "4eggh8BTDEBt7Qt/EYQ621PHdFcgKAB9TAdgyR8db7vs8NMPf5ztiO+7wMt1xx29cW+wHr9H6a2P0h3fdsU1AYhO2a+dEd30u3r8fV8Inj4g29mEwSmOxzyn" +
            "Aa3zweplfBV4uRFo3YYV7QRinb3XXv3Ylx379a7M2Zsd1CMi+/irW3NevRuxC/fvxOsu/XW3HnuzR3DecQTqcRRV5ySqj1NDzulxHhGlxwV+UVeVK1CVq/+g" +
            "K/ew/4+hKfH6b/Ve8lc8BXryForyEX3IF2TqB2qdhdu5ygkRkENnd5MHfCpEvFAVy4BR3mBUHdIQWW8GRrXWGRUAPvVEt9IXbBoMLo1ExzKOTCRTdSbNBZYW" +
            "QnecTFqja89mIG67jr7dYNJe4PEA2PSXCh0HVv9C8jkdyRfBhOi/45LOi98u6bzukk5ms+E3tvdhxN3g6A7yJ56yVY8tuuvZpKN8PVmHeayB/1kJhViGjsqJ" +
            "9wWY52zMdxo6rYmoA2OwhjBUheE66vtjbUFYYw+sNAArdiLfF9hvqqO/3t/qUYGUwy6VRPXxgoZ4gg0FER56J5gX4Q6lzoFddXaKBiJhjyl6eMadTLFg7zPB" +
            "l+/oD78iG5/Bm0/oED8gP++QpxTUutfQoATk7xny+AA5vYnsxiLT0ch5FDBwFmg4pf+OMhL4cOrSEShThP4bASe6dgKdW4DSdbpuLMtWJqcizQPz5vI5iNmI" +
            "WQjnHyVNR0yFnkyClxkPRo4FJ//Qw8nMUXh1vofpv+/5HcN1rv7+rc9/xmD9GAzuOGOwfm6ofm0o+B2GMf7A6BPxpOl48ly9/i9DRXB6pk2Y8w7wYK/um5zY" +
            "j8QKz2Y7p1h4z5v8NvTgL3Q/0/GdBHS/wZ6lZuP7C/b0O3Y3k1ux1w4gnEEvBOTgdx+fE127G7CeF5nyAN4LI3teiGLAfUloifMvtMujN6kETakCFnijS6mG" +
            "rNeA06qF/DujDqIeor5+1M/+/DsaZJ//x3O/zzu51BgK5QNGNQenWsKVtEa00f8vRluEH/FHtAfunOrVCbW3C3AYCA3rAUz2Ir2BzT6oy8FAaV/SDzEAMRC4" +
            "HYwYqv8OIRRYDkOMQi3/A+geixin691EuKPJiCn666Tsnybh/ATEeFw1DteP0RkxCtweCU6EolsYBt0coj9hIPjRH08N0ecQhPn0wswC4bI66z7LH1WiTTZf" +
            "mpBG+l7Uxs5V01Wjou65SmOfnZwprPMlP3qNPGCLG/Lymy0yEZEtjrw5uWJFz+hkyjew5CMY8jabH69Q1xLgmOOAg8dwCA+Aituof79ZcgkscXq20zpHTiCc" +
            "LDkKlhzVeXJIPw5lV+ffdfkA4uDf9doZh/TrnHcd/x9xLHu0Y9nj/WbegezK7qznTne4FRzcqr/+B4QpPXM=";

    private static final String MILD_EXHAUST_REVERB_PCM_ZLIB_BASE64 =
            "eNplmwV8VEfX/+fubtyFQCBYkEAIQYJrobi7BC8UKFKKtECRAk9pKdAWK1CKBXd31+AkQHCXEJyQEN3dO//vDLzP2/fz53522ezeO3Pkd37nnLlze14eFxfK" +
            "kZ9XPv0pNK5oXIF1znpzeib3TOFI7vmcI+Xze0rPF//9+1nPJz3v9bzeM6nn3Z4P+fyMQ539v1e80K8U/Tnl89Upn4/n/x3x38eL/++b/z37xefz/2f0f4/x" +
            "v9Kl/J9R/y35888a/fu3lH/J+ulz8n9H/x/dn/9Luhf/HTPlv/Ik/2vOf8+X8tlKT7HOzZ5ne67rObin7NGuR9PuabE/dc3fOb7tx8alq9vCn8u298PPeB3e" +
            "tC9x5/jNs9aNXb1w5eoVNVaIFZfilsZNjhsa1zGuRlwkfikUVzAujJd6D4srwJH/v0fov/4P/dfxP9+G/n9nfPr0aQz1/mnE/xnz0+cw/SrEUTQuPK4kUpSN" +
            "i4mrHFctrjoyfRnXJW5M3Py4LXGn4+7EvYh7FyfjSqzovWLRioQVYmXkys4rl67MXNl41R+rrq8qv3rF6pA1S9ZMWptnfe+NnluebVu98+buG3u3HrhyZN2J" +
            "Hadqx289e+ZitWuv70x68vfLNe+Op/XLuWzc9FgZMDK0YNFzESOj0qK7lIsv6x11oeyJmD9qHKybWj+pQWKjPk0nNV/fol/LoFbbWu1sXbvt1nZb2t9r79d+" +
            "YbuD7Qd3nN6pfOcZnW91XtblateHsX90u9Dt526vYv+JTYgt0a1Tt5bdKneL6ZavW3Ks0W1qN+/uzbpHdz/TrXG39NgPsZ7dsmMXxD7sGtX1TZcqXRO71ort" +
            "ENswNjq2XuyY2OGxZWODYl1j13QN6jq3S1LnUZ22dSjbvm07W7ujbWu2ndEmufXL1ivaNGk7om1E2+/atGp9pWWFFp2b2Rv/1rBpgzMNGjUs1eBKvWp1L9e6" +
            "UqNg9ZQqyTH+FV5ET41aVLp5yWnhuwtdyj87JDjo54AM/0F+sd6L3JfZVosd5rfihrWmazPXCMt3OYtSS74p8brf6w5v3N4sfXkgZXvK4ZTGz4s97fOgz60n" +
            "V6snNLw4/Vy1s6vPVIv/5dS9k91POo83On7kxOrTRc5WOLvz1Imjaw59PPjToV8OFz0aevzy8XnHrEevH25+qM2BI/v+3NfzQMjhXkeiDjc+sGbv/j2Je/8+" +
            "0Ie/Cxz55bDn4dxDnoffHbIcnnZ42ZH4Y8NPtju17ET6sdnHJ530POU4Hn6k/AG/fVX25d2/an/ZAz0OVD1w88D3h0oduXnUcbzeqT3xf52bff70maanTp+s" +
            "E1/q/LHLZ66euDbsyrHLTy/NvzwqYdHlBxdyzi09//KiV8KChMIJFxKuXDVuJd63Pc7zqMT903e+u1Pw3v37vz54+WDKwx8fFXtS+1nrlAevf3x/J/VYmsjq" +
            "5sgWy6zNbfOs/pbvjSRLmnWkpaJIE8JlqkdR72SvJh7LXS5aEi1bXOI99vm09o/Auv4B5wND8izNcz54btDigD3+4UF78trClhf6s3BW4RpFCxf/JqJcaVF6" +
            "RcSNkrNKDYp6X+5ZhTkVx1f8uuK8ikMr9quwp9zWaKP8jZhLVb2qj69ypUKJ6IFR18vMi5oV/bacZ7kbUV5R7aInVcytXKtq6yq9KkXH+Ma0j3kXM6xSeKWA" +
            "GFvFwxXqVyhUzjVqbemo0t9H3i7TMqpaVNuojmXTooeX+zE6PPpeuQkVm1ZqWql+xT7l6pR9UWZt5NzS/5QuWyYpKiUqocyFyAqRqaVLRB6MHBg5NGJoeHKh" +
            "toWHhhcrsajE/GIdimQVkkXOF5tRPDN8UJGthVYX6l64YKFpoXuDuwe1zfM8X1KBBmGv868NfRT6pMCNQreLji/WPLxeoaH52ged8l/gO9FzpMsNo6Zx39LB" +
            "NtI2yVbUdbZ7sOcQz34ehtsxW6Cto8tTt/MeO92v2kYY9Y1A22y3x+7ebjetXsYGs7NzgjRcsj2OeRX3LOva3ZrX9rvLQNswY5mcJsZYHlhOi5GOItnfZn6d" +
            "WT+jW9rrd4ff9Hrr/iEyo2Vm+YxHGZdydsn3LnEehTz2uy6zzbZIsVdsE/HyF9PpXOmMdgzILZI7KteZPSXTN7NGdkf7TOdFR6fs52mH3jd8v+19nvct3256" +
            "3fH18zcxqZfTu2VuyWqSfTi7Xc53Oeuym2R1z6if3jytQfrEjK1Z9XMe5vTJKZnzMfdXc64RbySZY3P/yvzj4/CM6tk/5kbaj+aOzC3seCBjrbttz6xVLbuN" +
            "65YGtnu25dZfRSfnKvsje1/nI+dGe4/skZlFslbmNpRVLf0sKWKXfCKfiNrGHbnBaTpOmq+M87Y/Xfu7hbrttE0X+xz3cufmvs6taD+b2yPXzZ5uf2YvZZ+W" +
            "65obnLMqa2umPbNwzp/OFpYxthG2ATYvF3fXv1xnuoRYfY3FwtNob4wzAi0plnbW7tbe1ofWUJdVWLaLdbLN6pblvs7D8PB0a26bZqySbeR7mUeEyeJmI3nA" +
            "8qfrKA9vr0pejz2rerXxmuoR5rrDMlF0F1stNV0PuguPJW6PXCJd6rtuco/yOuP9zjvTa6jXc6+/vaZ43HedZFthaWIZYI2xXbQmWaSxxNhgTDS6iUB5TEZZ" +
            "e7kn+QQH7PNv7uf0Xek7xCfB64Pnb16/eC33zPBK8B3gHxzwi39bnzPu511KuFRyuWXzd/nSLcUzj0+89wrv4T5nvQd53nHP4zHPY7NHgNde32WB3wVZAi74" +
            "1vct7HuNX0d71nY33QI9h/uE+f/o3953uudANxe3Ce7Cc47XEZ/oAI+QmPz1wp6EPSlwPl9C8BP/1j4vvMb4jgsqnLd63sTgvYGbAzsFjQ80/Ev5FPL62SvI" +
            "LyKocXDVwHi/xX7S3xkQGPRzsGee9DzVQ1+FLSnUq+DifD6BNp+3nlc8UzwWu+a1VXJ95BURkBh0MWh9wHTfFO+/fXcE/BE4O6Bk4JHgpyEvQu4Hxwc08fvB" +
            "9y1a73Cf4drPtZ9bUfcT7tc9tnve9jzk2dtzjUctdOvm6etVwmuB5weP++4PXJNsT6xnrGutHraBrv5eff2bB9/PMyLPzqAvAir47vL0c+/nVtG9rEcZj1Zu" +
            "H61J4pH5wJnqzC9vi6rWjq5DPZp52T0T3E+5dnad7DbCo4PnEvdfXRpYG1i2GDdFD2lzitzszAUfd6V3y6iZU9zpJw+a45337DOz76dvT52f+i7tQub53H2O" +
            "Ovbe2Sezdzi/tky0zDO75yzPOPXxYUaDrLNZ17OCcl7kvs99ku2RGZ8emb4uvdnHrI8zMv7O7J3dIWdMtndmyfScD53Tf864m1k2q0dWRPa17OHZDbIiMsql" +
            "dUg9mtosLeTjroyhmQUz52fszZiZeT9rXU4J+1R7cXuz3BPZQVljMkdmvcqumdMp+4+sKlnPM/NkhWQH55TN+SE7K2tAdljOrpztuUn2Fo7hjpOOaw7TXiV3" +
            "U/aXOd0cV+Q44WFucITIHGOoUd+cZXex93P0MqfJQvKSM9FpFdeNkaKM87R9tLOkGG18aTQ3qlg8sfNC218uhV0LuO5yeWUbbw23lLS0sFawVbPtt7a0lrFm" +
            "Wnythi3RFmvrbh1jLetS1M3pZrqdcBvqNsrN4v7OrbvrQZtpu+Ga5ebhVsqloM1ic7Hlt822OW2LbEetcdYptrEuKS51XPe6bnWNcfXkGOUy09bG1t3Fzc3d" +
            "Pdz9rZuP+x33+x4vPMa5x7q+tC21tba5W2cZdQ1h+dGywLgny8lBoojliKWl5aTx2Hhk+QNc9Hbb6frC1tz6j+Ufy3VLUWsP67fWGtbdllqWt8bfRmGjoagr" +
            "j5mmOUqukj/Lb+Vx2UR8K7JkdbnZnGpuNFPNanKyfCGXiSVGd8vPlnqWy0asUcS4IYJES5llPjFvcU6o9JZLzHCzmLnGjJa95JeymAyXUbKarCjTzIXmEHOc" +
            "ucW8ac4y1znHOsbb7+ZuyzXsM+whjg2OGs51zgxncbOyWd8sZP7kjHN4Ox7bX9ul3eT9nH27/aj9of0Nrwv2+/YwR6yjlSPBXtPuYa/KCEfsy+1d7J3sp+x9" +
            "HU8dfs7HjlmOIY61jgBnrLOlM42/BjpmOE6DgFWOrx21HBEgYZU93L4ud2ru1dyecGY1x5eOog4fR0FHY8ePjt8cfRyRjkBHMUdHx0RHb4fVccx+1V7LkeTo" +
            "5lzl3O2c72zrdHfedBx3HHbEO1IcpZ1jnPudD5xpznxmT3O3acgyHEKuM2uaCc4BTlfnAccix3bHR0dz53LnB2cjc7q5GGt8YQZhrTHmc7OVnC+3y7PSgeX/" +
            "FDvE36KHiBYhIkCUF1+J8WKmWCZWit9Ea/FOzpWj5Vg5SLbF18EyQJaQrfDaHnlPZkpXESqKC39xT66QP8h+cqicJxNkoGgseoueopWoIYoKH+EifPm/gqgi" +
            "KoqSIo8Q4o18JtOlG1cGCFfxQT6U9+VraROFmL+qqC5qii+4NkJ4iUy+f8yvt+R5uVX+LafLqfIPGScPyesyRX5EA0+RX5QSMYzvIa7KdfzeVzaRFZCzhIwG" +
            "E2Ego7fsLyfJ9TKRK0JElCgjwoSbeIEFNstpsg+/D5XD5HA5BtzNkIvkBvQ7xq9XkeyFTJU50gPrFBKFefcRUr7j+8vysNzIuX/KX7DHZDlR/sQxmb/myOVy" +
            "izwgT8uL8iaafpTuoiCa1UCreqI+7zVFaeGJFe4zyklmOspxTJ6RSVglVORDNqswZZbMkB+Y/b1+fZRWrBWGvSNEMSLBRdj59SXXHCSOfpHfyyFyvFwid8hT" +
            "zPqU446e28l1bljSTwSKYKwfzLX+2vKBvPyEN3Zz4/BAIh885cN3n37/9Kun/t1F2ISFGV8y7kv0T0e2XMZW/6x4oLaoiwfK4d9QxnDnbCGcnJHNkaXfc6Xg" +
            "lzKiGpYoJgowuhejSmmCA4e8hn9vIfEj+QZru2nPn8KOG7DMfrlM/i5nydlyJt+cRLu3zBok2oLSxeIHMQistQVzdURlxi/K2HmQ3wMbZsvn8gFWeCFfMW4a" +
            "MmdpSXzBQITGnh1N0nilg+YsrZGJnBYOG9fbOFzQxgN5vLFbYWRvJgaK0WIYcdIeT8agcX5G8gBNB+UupLuELlflbebLxE4vmfuG3CsXg4+J4HYJGJnD3xeQ" +
            "Ky8ydxG/iqlI30w0AfllRDj2K0qclOH/QGGIV2igxnjO6yWIf8p114ixRKz1FK1SmSXnsyeUvK7/x9ch2ttB/FVAROKd0kRKFKNX4D0cTIbgo5uM9J448gZ3" +
            "YZyXD5xXwJ81RSWuyY9mGXhln/wPcdKFGB+PJ5bJTfIIyEuV5bB9JBbKK3JA3nSibLQcBRdMQtNDYHoP8bZdPBdZ4qXYJ5aKaTB9d2z3vfga7SuD5kCkVpZ6" +
            "IZOpPR9pLniIzzKRKVsfGcyTyhkqDu4QVyc566M0PuNVYdUPnf3RpwQeys9RAO/WFS1EI7gkWDwikg/BUHNA0ATZg4wzC+k2gSsVoVew5jnGvQ4/rJd3sWki" +
            "3z7X7OYHimPEPDFOfAkr7oUXu6Jvd6S/JSobUUayiAMJIzhjGB7zY6Y4GHAR7LoZ69wBB0l4/yZouAuLKSb5CNrea02ydFwKsO/Egx/R7zW/qai+yTXPwc9H" +
            "NE/nPZer14H89XI1Uh8iUm7wzV2slI6M/lrfIMZKQ5+JsrYsSjZoJUfKv+RONL+Mp87CX9HYOljb+qmMR8NQpH4rnoqDIlV0MX41phlbjJfGKqOE8Qit/gPG" +
            "rWIk3j7EKEvkAiJwKtgdIQfAk5Pg4Dkw32JwcQn7JenYvQc+c8FRAa68yrxP0VIglyt/G8z7XCP2Axq9R9s36PsCPz/7HB9zGXEaLNZTNuTogfxHZFGk9hG5" +
            "+Ps6nNCFrBLDaKnSAnqbiL4cHckyfeCA78WP+KEdmC1C/IQxWxJeXip7imNiK96yGC/EBrEZjd2NFHijL1GQhU5hsgZZZYP8R2u0gqx4TdrxTBbyPcdnT/n/" +
            "HVK/1Rycgs1vUq8sIRqmMP4qMHSZiHzJ9yf5tQZ4C9C881HzSZYsorkpnO/uwg5LyC4jZHl5xvzb3Gk+MF2pVb6RK7GcH9nxgPjK6EOdk8dwMVLFNbFcjBUL" +
            "xXny80ZxXVwVJ3k9Fr5GI6OBUdB4IKbj+3NkoS4yxVxNFXTXtFIBVZXtydVleak8dBhMSWLIQLIp2Kg1cVFftAHPQeIjet0i65wiTveBrFUg7IEsDis8JwYO" +
            "E897dAyf5Jzr+Pkf/D6Xc3by/Wn85sL8nuj3CqseRMaxcOEjftsL7h9w/hFi9QQxXZRYHy8WiEsiRxQwPIxLeGK++EkMFt+In/FcJ80GdfDuOL79nb/SqYuX" +
            "mmdMd9kJtO+RdeU7cw9133fIm4yt34LzJPMedV8TkDIRvx0jjpPwxXUwla458S2oPw3GDJg7Ap1jxRDxHX5fI/IaJgj4WpSFNRQ6M/CwAc/Z8UOCrkRyyS7R" +
            "RLSbeED+eSxrw/kTQM8H0dTYYWQY54wpRifD1zgt/hL9YdWl0pAvzbzI2VnWkTWpNobIgcTsQ0aOAKHR8PuvvM9C1jXYcKxMNROdbx3nHBHOv52F0a00+Etn" +
            "ngOimdHNqGaUNnLFUXFWpOD5K0SpmmkEfBYiUojevVg2CXzeBo0PiLzbyJwPvusiasHFgfzfmUgoh97haNESzz9H8hqGq/ERFI0GAwVA+ALQ1xS2uoYnC4K/" +
            "NqIB8bSKWfeDvWFc7U4OKUKWm4+9osRjbLwDfhslBxM52+R3crvZ2PzDfGE+M38z15r9YAirPGruB9nFqOabyc5iKfW+KxzXHK/+R0yC/6eBwvF8HoEvviY2" +
            "ihAx1UFRCyJScVQA/ipNltiMDN8glVXch58PYc2vZWFQfVwGI2l/rmhC/mjDSD8yzg8ws6osA8lGg7HcTXT4B0xdwkp1ybB3RCnDYozHDzXgyW1wVz9ZUN40" +
            "l5sTzDm8gs2WZhhRfVoGIEVlbFcYu31rjLYcNi6Sqf4BY6+w821kaSD34bF4M5ErpPk7FjhovjJfEtGpZl+sOlJGYtnvQU5RYmKdOCfSRIhR0ahlVNVrID8Y" +
            "g4wxRmcjwHguLhIL85BvIFnlO+qixXDEI9Dro6vxguhYXtzBx25oVonorU3EXYUt/wTxH2Ue/m6JvEJcgMM2ydYywbxvFsD2naho+8t24K2bvGPmmkNgKjei" +
            "oKT4jbNGyNtmNXM2qAuQf5nfmr+aq2RFo7rxWJQxGiNnXWMmHd45o75RzvjCiDDSxSkwkEcMx/fnYRSDmAmAbfvBvSOQuyfR1RcPzxHbxFB6hjQZjZ0bIVsN" +
            "crLik0Vk3v9gm3g40Y2K/zisOwUc/Yw9d4LCovQve8xTZjtid7BYIhqK3XCSBzWR4ogwpN4mY0W6qGTkJTJcDG/4fDM4qiKWyKnmK+ciszG9n4XoW01O7Agn" +
            "DbF0s06wTDTsSNUaXJ2AdyXdZSQ93w/mIrODrCeGI19zokH1Mc3pgfpxDIQnhopfyIGL8cw0YmcxLNyFPHSTen8ZMXAKD3+PD1Yi+TFi8SSRe4EOdDDfzZEL" +
            "6VPOkjfeo98t87pZBxYOZNzp2OY0c4yWbuCjHAwXzRXr4Va77C9+N65Z1ljeG8uMOuTfm6KQ0R09vcR0+SMZt6+shX3u4alHzk3OfU6bOc3sCk7S5Y8iFL5o" +
            "aRQzwow0sRtrmchzGpx6YeUxzPLMvEp0BpjN6ZrL8c0p+YW4LPIZIXj7bzQdKmaLPeIEOCwr5lO/PMND+cBVY+K+OhzgCxv/Q1wnmfmoKsaBI4esT/x9i0Zd" +
            "jc4WaZQx9oHSm1w7lP4njswRInqJMVhvB+eU+9xN+Iq3dHhVOdOBhHtAwFxZX14265odzH3mAfOEmWJmmk5zlFwj/jG2G5OMQGMaUdzAGG+sIV7WiofYVdUQ" +
            "JcRkav4Q+PoNdj+DD7Yx1hTi7TX8vRcftSeyL5nNYamlMMcjWQyc/gQftDaKWjYae0DDWvJGEzLnHKpBQ0Yw8gis/hiujYf/Torj4j5snwa7HAG3Cqst5Cti" +
            "ZqZzqnMDPggyl5mCOCoossV24wF10yDjF+M6ctfCE57GWPSNhEdKga1e8pJZlzzQF4wNIsfVgYEakN8zqYk3YuMncGp5pLgk/fBhOeodP64shp2eo1cx6SJz" +
            "zIvmQrOTWZl+f7DZ3pxkppkTyIR5ibDBIHQSfP89HN/LiDFGwhMtRVP6rILktPtIf9I0neHmbXMyPP8NfPRAlDBWGH6WRKOj4Wck4f3JWOQjPeuX0lv6ye54" +
            "UNVmD6gcyulOqRcxNAOfK+8VEUlUt8ep81rTG/0Dx9jw4may30vZHgydweaXyEkHqTF3kdVm011/yf+pVJzJ/P+TriO/Ild+D0+oHsATtP2N9AVgm1DjKDlr" +
            "M1n4S6Q6KpLxyHGRJPIbA8iKb8Q9qrkWSPALHukoXWVJRupEDVeGzPA3tWUEuAgkg6gcmYr9LoOQv2SsXEsWaKv7d0PcABMH+PZrNP0eCw+Cz8PNNuYm86h5" +
            "xbSA8i1w1ClRxdLE2tT60jLV8tboZXgZO/DcO/mrLEAndJ2qwJ8OsAdWGSaOiAqGYfzO59/FbbL1LjqFOnSEL7DSe5jvLfZJN5uQEXsQIzVksCzO7Afwc0Vq" +
            "jBQYoDRI72e0M7LR8Ij4DQ++h79LI+EQriqJfk25LpK/f6cmbwRig+RTM8E0+E5p7wf3/ITMCdSHLYwRRMxjI9l4aOylnjwqDostSNYV7e9RNxQmtiVeGozN" +
            "YuHLmnQAMeSiycT0JI5f8LdFxDFjBrhLNcsQIzlyLhVMIaR6Q2W1D6t35vpm5MLu2D8a9KoM2JoK6hYZbDZS/k7GvIgPfOgmG4H8SiAsHo8UR7f6ukPrQkTm" +
            "NUYb0eTlhzBLFZlsbgGnDWDMM/iyDcg0jKJUa6vIW7tBTX+yWRNZjRlHMEcCXLaO6kVVQm2pAALxrClV1zuIyErhqh9A1kjhajQnq/kawUYs8ZGONQaBhHWM" +
            "0loOAweDpb+8YA41i5gdqVzqwbXFORxkzZKyCDVHJHnlqRkif4Nvxwk/5M2hKt8Am7c2rhqJxmTyup9xl1i+ILyNklTvi4jqaGrsaqBtjswnn1AjFWOuEWDn" +
            "pmmHu7cj+1Zqwf/IliA5WHQASRPpkN/DQVupUe+ImkZfKoRN5KiWsOc+eqM5aLsahlUrPS3JQddgwm/JVq/Bw1syjQET/ilWwjNf6HWWMUh4Dlwl8f9cMlA1" +
            "vQZQgty9h+j0pjbZK1ZwVneiOgdfjUbTUhxlqRpuwPJqFSEKHDvhgxtwTXGuPEbXM1V3alnyMNVLWTEV7qpAlnpINJ8lBrsS7SOJkt7EUR2ZX7pLT7Aawrsv" +
            "Z46GGYqDhqHirphvuFjeGGvpZraTh6WsTNwI4y05oxbWCwAvwdQYc9BoNfgdg+wuQnXJarWpILyaF08LqtI7dNDu5J+J2OErcmWUDJRezBbG3A/pIWZSJV+k" +
            "N/MnU7YDCakwZiU4ogH+8jO20it+JWNkc+rZ7vjKYQbJxviqF0hrw6ss2rWArb6g9llELukudopEusoBXGeXXfBbBDmiIHm2KVZNxSdjkXcRmSNJHCY/trA0" +
            "sTSwPDYaGgeplFR/WlGvhqqaxNRrRI+xbyrcWUBcwaeJZOAcvb5RmIrrKvXEcGYvCeqLyLxop1bMO8pKWLQAcrahV16Bp/KQ7XPpW/eg7VE8UQavxpL1FsMn" +
            "z6l95xlfGn9QT2yhA93FDPfwVDNGLEUczyAG+uH7HmgXwEjV8Xlx/s9HH2vCm83xR0u6tGp8WwMNisJ4r6kgLMSUn5FIRjlFpnwlXsB8o6kWlhFR7UFBA6Qb" +
            "zDxHwIYkU9YQ9ahnG6N/PnEdBu+JJr/CZrVhj4IgYxe+rYefe5JnSnPOA7B021xgTjHXwy2Kiy6a4VRdxcRS4WVE0sf/xGjbRB9qpQ1k0Z7UvR31KtsLvF2R" +
            "/GllTlWDhoObM1Qde7HVDGSLQK5M5ipLJr4Nnz2nqrtJl1yCfmIo/cQOUcXYaJwwdnMsMQYaxZnrKPF9hrjvR7z9Csa88UlnYnEj3kolmvfDD15ytFnbbGX2" +
            "N5eYN8y3Zgks60OEBhq3jNfUMCFYKwmGdqEG+ULcpn4cixefwLFFQVlxPpcHd2PhiR/ARDiyT6QaWUiv/oKq1gPEPabKHwwPzmHeFXI6UVYV9ooki6XJatSu" +
            "P8HS/aiK62KjUkZvKvm81FHuzDuHymMUXewEPFmGWmwLXXo01j6GRBu50pecmSBbEtNXQM0wzjuBT69xTX5i0x1U1KPiOkufMZ7qNcn8yxxnTjeP4BmD6KkI" +
            "a+6ivvMgLq9jiVpUue7EzSlq+BQ44ytkHiS/ISu8lh2oJX4iV64l4xcx3jP/eDyUH6+XpPZRK14v5BP8NZUo3EQ+OQi7dCQKlsBbF6gM1ApaIp9C8VMBoym9" +
            "yk5jAlXXfjqFMcRjmNjGlU2knco3xcxDlpsER7Sg9hkl/sAb5bD9aKrejeIgNeoQ0ZvMWJFYvAq2VsHJQ0HjJTrDeNOHuFiJjf2Ivv7gKktaqGXrwD8CSUeB" +
            "/JlkzYZc3RemCoPtn+EhtV77lkiqR022gS62jjiG3ovlbmzXQYbCUgvh01dU6VPRyUac+2G14/h9DrGyk2r2A9JOhkH6iFbw/0q84Elt1NWYSE4oZlipEk/A" +
            "MWfEW2EnYxymXujHefN4ldf1xFOkuK5X4BxwlBOpHVhsFppUg8++lKpX8pT3zaXmXLA3QWbISHw7Fy91Jzb8idyfqJimkNdDqO9WUO25M8d6/DYM1mxG1vgI" +
            "AySKS9RtXyJ/DDqWBUGeek3Zi17JC4/WgOvUOl9eatnC2KwznBdFplFx+E5WgEGXimV85yPUfYz3WETd0UnFy12JrWC4aSx58irWvAiL/E7nWYjZmpAbgpnH" +
            "RZzXq3bTiMJydBI9yIxLsd4j6UnukFRnrYnZQJghm9i8wijXGWUAfjwgG4K4+eI63HiB3LiPXq+veMqsjRjjT44N1BSuSF2WSCqpvZomfZnxPTV3EjL9Snxu" +
            "p9YcKLqBWrVm72DeB/ruxUfOOQdW/uFTPxDmRs3Rlr4/zEggC0+nKtkJ9qojYa72jlptqsw4E8RUmC9cJICKr8idahW0GZl0I0ywEGS4g9kfYfWaSBQuUtHi" +
            "Aj2eJ1j/hjqmA5p1omIbAM5OYt3y1GqvwasL6NwqFoCjUeB9J/reQdvWcLgvErwmMi7Q/c8111Cf9sTWxWD5Ktj3KRkinFpwLuz/Huu9gR9XENcz4I1r+s5S" +
            "a/KKHT48QX2+kAgKFZeZe7MsgE9vYNvbwimCqdgXwEX5me8dVw/nNY1jEdXbeHRrD2NXhv2PUHd/ME2zIggdRwzupEL3wyc/04l5wV4JYgQV6U50tlM9ROEX" +
            "E6tsJa8UBw2Z1KNrkKkWGP0dK/XAJ3nQLwcOWU8+yCuzzcIgX1Xfw/DcBHByAn0+kpMawvcrwXIA3fZrjvvUSzOIo2F4aqZek6pCLoqgj6tLjA3G4+34pixe" +
            "aAE7x4qv9fpLFP8v5JuD5IMpIHUsuag5cVaZmc/qvtabzL0Gv1Qlf0ZQ7U0hfpOJ6uvUev+gaXk42R2eGMd5fekNfiaLHiEuXcQbqubTRHIKNn8Ohq+Sb9R6" +
            "pqqYTsFOm+GafGCinOYfL3znwmsA2D+HPSJhg0fEVi74KUCUqm7/EJ1nGshcC38u4ptq4hWYbwmGvtMrxyEygPcGRENzeOAujPyEOiAvdeAN+P8mn/Mh6yvG" +
            "ldj/nV7DcMXDefGK+u1L6utRoHkiMiUTtduIheXETS7n+8Ef46nL2yLZI/wwE2TvhJ9cqe83UV2uRe8tzOGNvWPIJCfgAxcd75X0Cu4QepMGXJuHb3qJv9Bm" +
            "HD4Zhd9Xge/5aPwjPq1OZ9YYv+UXNnqC/cTzH3h9MjVGHelBR92MDm4CnhrNdxtATgNy3g1ywyDYRa14qPWpiXIgjHgJ3XcRJb8xyi0+J8Mjgig8RO28DuZd" +
            "hwQLmH2UrgPj0eYvzl0OttQ9Nleh7gscQa8d6PUns83BGjv5NAgJ/mLmV4x4B9so5jPpzBrAku30GoCdGkata8bR0z0gmvbhKVWZtQRn5fHFIaLoL5hoIj3E" +
            "Oqy8CP7qzfsp5luFXVWk7uXvuRy/gqh2+CkfuG2A9L+ROxx0fbvEEuoF1cPORIt52p7qzs8Y0D8bu9bAB1XJY19QjZ2BW5rSefwJt/2l78TuYa77ciC9bwfD" +
            "x7gEW1SCB++D1hlUJGpVWJpRsNIAurYeRMZUjulcOR6JV2JhT+JpIblkL6x0BKZpSlVyR4ag9UG45Z6utuL5viGjqrtzNeU5c7t5z/QlskYhg6qaa5FDjoK1" +
            "L8CD4mC1VrwOzjwP0sagQycquu+pNv6DhhPQtBEWKIJf3mG/VfpeeZK+33YKrw2Bv24g1/foWZ1utAX16TJiNhUbNyMqhzHrDK65SyypPQTRRE4NvPE1uXo9" +
            "CHpERD8WT4TN8DYO40e1o+IF499GqxegPy8WPSkeou0qbNUXhOdFkkxGVKyxDE7sRAUYSNz9TlRvBnHPsFEIs7QgF2XAwnPxcVvkmMuv16hu68Ijy/T68TA4" +
            "pTIRnheM9KGzroTHnPoeaiZnTifjR5DNGzFDf44B1B1OOYF6/SoZfAlS7QPBqltNIKtkyNJIPxyLreLXSfruWndGPomE8/BfFxlL9kuVQXhsMHOVpxpS+xjU" +
            "fe/lemWgP9E1C56fhL3WEz9HqEee6HviWdQ2z/DTEzJqBP6Ygu1Kw5tbYbg8MGtTonwErLQDVl4FQ00lvhdQQy0EB79RwTVAvwawR32izYq0a8GSwmUZWD7V" +
            "vGbeNF+bAdSjbckus7B9MXgpC3aqSWQ3gMGnkVMGwRwd0ehvKpgroG8Z8fsfoqs8WfUK3dM67D9bR49aw1J3o28j/200vA4aEmGtyTDlRDC8TI6BJdS+kRXo" +
            "E4SvKoC3ATDfPLEJXCYw/mX8vQHJi4jn1Ndq5XMno3rSU9Uj6/9GXT4GlNbBClbqjARy3EpwV5+c/jOs1RctgnR1FcOrHr3lZKTvh1UGYfnv4MNi4i6ZZTR8" +
            "0of4+ofo6gB3t4Zh1qN3FZBQluxVAsQWF+pfCfxcnxHD4ZgMKoj86K32pJSH09NkGFhrh2zqiuJU8/fIrQvJZ8PQ+AfYJF7XV82IqM1g5xavRFhkHFdFgNAL" +
            "+HoRcdSWiJ9ChD0h81jIS1+gYSU6wzMc8cj1Byg6SIdbQtchZcVX6NJK59faROYpWP8KkbgHtP2MHwdRlXbAyou42kqcS/yg7tieIsabIEsFst4T6pX7XHUO" +
            "+UL0veF+IKk3Iw7GI8XpoHzJhOre22kiwoVMpe7mJnPNG3rlenoXk6oTJ6LpOJC7kBkSqCGjmU+tJwp+zUSXovBvb2TtCEvOoZ66BjdfAalfo/8H5FJ3+neA" +
            "+xF6heowKPoDvOwHVS+oyVStkcA1u2CmeWI3uBhErboXqZai3Ui6nP7MvRrElcAfBYlnLzBxlSy0Hdbdo7VzAymfdtuEwh4uem9KMXw2ChwNwK95qQmDeA/A" +
            "nxl6V8UjYtWdc5UOt5FuL1itAUsMgveHgKjhZMsa5JxSem9RNNboT5QsIBuoaBlHVVpEnABXvfHDeHh3PH19bRilEyyShY93kN9uEBm+zOoCii3iCnF5nkh/" +
            "D/Md00yWLAOpC4bBK6qXvUw8FceK7YnJUnxjIuNpYuMhfcBZNF0G6q7onS+XiYddHAlUIbnYPxA/5UNWtV+mAvjJz5z5iHC13pvIGfmwiiHUHaoCIL4C+vhQ" +
            "v6p79luIq+2MmknVVx+27I7uqpobDLvMQZKi+O8mWqg7iA2RsyEWvANzncL6j7kqA02P4KXJZNNNejUhjrgfDreuYuzdxPRT+EZ5WqE8AV1U9e2NRd4yxlyQ" +
            "NZ3c/xFurYz0Y+mxD9D5xhNHi2HBLkRQe/Ryp2pN/7zvIw1pXJCrBtK2pcJ7r1ezL+m5r2KL8uCgCVe14r02tnChJruBvXdi8aVyAVJdoWaJR+5QrFwbXp0G" +
            "n/6CvztQrw/Dr1W45pjumpLQUJ3lJ27j1emwzgx992QDtj+B/m56V1cwUjQkGywg362k0mqAxDHY/xXIT8Tjifj7qd5z9pK/TpMRHoAB5TF3rHsZiZSlduqd" +
            "Nxe45rXeefWB8U9gRbV7SO0fDKB2+xY5l1IHHKXCmgU7T8Riw8gXrbFTfd0BqHN+JzfVBNneesdgJpygmKozbPEVDHMR1GXK9lR2NuMGUntST15g7oX4cKz8" +
            "EQspSbbq9eqFfD6s11TW8N19NAhF1+JYvzJ8O5YsOBpMlARZp8D2PvRUd6JuMUc8n9+g6TZdF6l6bBpaPiLzSnr7vFp39cmfHrgM1qqLFpPJObvQbi/HZrTo" +
            "TreWpneb2KW6T7aHSmQ5Mq3Hn4nY6i7+PoBlbuHdS1j0LjKq/ZvZVIxReOUlMqjq1IGf3PW+OicYek+n4IffksHLcWReLuej8yaQ6aVXIVthzRjixRML+lA9" +
            "3QH/qTBsGH4dAkNtpSrYRVU9B3arxTiqljlMBfsV+X8csl3RVasLmDFAzS48uAErbtfR6pBqx2Mljq4w3RJYpJ2+v9+Ev2tiiXxEl4V8mw1b+yL/Cd0LTIHr" +
            "O5E/1O4sZcOKcNRv5P5ZvE8Dt910nqhDREQidzBSm9j+Acg6j6/tWDkEdiwAZ+7CHxvp9+cQscvQexaSBZG1Vog/wdI4sDua/5fBwKpj/AorZmDBROx8HenV" +
            "zpUb1PlhSOpFB3aPb8/glT1Y/43e9xJIB+JHd5GNxVw1Z7ZmlA58KoA1LmHx5yD7KTlwIXY5Cvf0o0ppSmZuAovOZA5Jf90ZK6/X8a92NgXDnalco660MnoI" +
            "Viqg97SFwJEWcPFKH2r3pQ0J3LGb6g3vYYEryHeZUXcR++cZIYMz8pHpvyIvdNP98Buwehgc3KPP8BOqVinP/EOIov5kyPLMlwqK1Y69DVhsIza4gk2TGXUf" +
            "UWNQLwSRQVVEp0uFLqcsRHR48W1l0VzXDGq3nAXb7wC9a9D4d2qzcdQN31CZDAZ918Cj8k8V+G8AftjG8SfRFQPvlGV8DzjiAfG1By1Upv6Z6+NAQQU6xo3Y" +
            "aR6cMEnXo92I9rrUim2oK9ryORwc3KOiWE4kL8EmDlkWmTqBtXZgpQCVRyKax+O9G4yn9hPf1Hso30hvXRmoO9Rq12VbInMquT0/XPsQdLtgQ8V5vcDKYJBX" +
            "D+xWR94inBGuVwHUXfJS6H0F3G8iBx3Tu3lVBbMfxO3C/+f464Bei3vAcQ9/bUK7dXrv2HV40oPxq+L/X9GsP1KXJnrVHlUDdHvwScW04tNEHfNb9S7F49TQ" +
            "Cut5tXTVyHIu4oPeZ1yMyFAMchv9LoLC8+DFClIc6JOoq/CT2EHtJFX7pWzIP0aMxEbu4qPejZeqeSOFM1R1FgNjqbU75emS2MLBb5nUf/7CjTx1Xv5CTfin" +
            "Xq85gHQJeo/vSzCj9ir7YpV3zFBEdyKvmf8Mmv8FP34nh8qfQNgtfZf5lebaonC40jUvjKF8W4f3hnB9B7zRU/f9g/BAfzzfGK0VG15BWrW6mAHTfIHneusq" +
            "Ru2gVXFTgtj1IaqH00ss1au+zcB4fqImXN9bEFTjN7DAdR0Vm9BiOrY9w7irySJzqKR/gTWuyerEx9fgrJZeTSiJZGrt6S12SMdzhRgtBlx04lAdxTdU5D3Q" +
            "oKCuv9S+VBfdU6gss5GIiPvcPRbTfXUUHKYiPplM74Vkdhg4HhnUXsrzYCUEvZprG/TURx/wXAutHoGmG3jyMfKfR8YneMuu+bYEfnyhd7BJ6QOy3YnY08y5" +
            "AwkOgcZ15Lff6TZV73JO7898Szy85txIrlbVpg2fXcce0/CU2vnmkEXIBr2wfneQH4SP3pGv0/TO9NdY/xWz34EfTpKNEpHlCrJd1vvPX+mdei+1j1Lwsx3c" +
            "5AOpEbx7wZE5jB2MvxrjmwlUYnWQ9jIyHoBvVpKn1iLxFb3/8oOuEUxZCg/3IwM0Ig5b4JMWSFaJCHjF3Gf1Pub7SHNfZ4M7XCP0/u5bWOAqtVgFPNQGlu6G" +
            "P0fAuS+Fq/GG2n0brNINOTLRYSb5pyv97WC8PxcLLESKk3pV9znjPkUKKVUdbfKudoQHUyFUABEFyaFWvRO3MH/XA2vNka0OCO7E2J2QuSuZryyYDAYT7nrn" +
            "Sjr9bjT+bMKvX+Pd+npXv0JMBnZP1LXmEJhzDJWY2sVyn4qvhF5xKAO2spFEPQtQBoyoPW0FkUYi4UYq0k50X9/SEX6j91fFUXHth5l2g4It4FDtyHTqlWMf" +
            "skKA7iBCiVIV7Y2wUQ94taiuCdTud5NILoSVq8PaRYWf3ktucOTl2wh0HEhdNYQ8Uo35HyNlHNy/ghhaC9a2ElnxsN9ReGs+Wixm/pPgWt3btNB9vgR9efRa" +
            "XX888i12aAYamvN3O6wSgwxn4PNZjBan1/lnMsoWmE3temuE3sX1XRg//cRBDmxwB7y9ZnR1N+EVlYXK077oYOeTyqN5sZBaZVO7kQw084YpQvFYReZtieaD" +
            "6c966r34DZm/od69E40/C3Lua8bMArGvwOQlMJXyGdcpmiuzpYqcNFD+GIa/zevR5z3spWGE+XDQImqgBVQxk6k9elM1W4XaIaz2317FH2/A73y9kvc3uWEf" +
            "12fBEvmwQXfsMhCLRCKDWlnby7Efux7SmX4zufII2eAMuls5Rz1P0Bq5C4Ox98il1n8cOg48dRWaIRXrnSf2b/LrB+Z9iszZsLQbZ+TBopF4OQiUFtErXCX5" +
            "/AGG3M8ct/RdpVuaMxPQMVfm0U+qvOHzUyxcAq3UEyBqdc1JH1YLRFfA7iYz3ERLVVmp/88h8XLyxiCwuZ5sGc93r/TasNqD66efB3irnwR4wLhqR7ySx1Oo" +
            "lQq1JqH6cfVXgI61Rvq5j9rorPYZSqlWu199fp7DFe8H831NvepXTD8TUYJrKsO9VeC7UNCThqWVFxI0c50ik6lnNFRHl4BVwuD3Umik9pQU44qK+omIAJCU" +
            "n79VBVKFUd9g01uajx9/nlvdJ1QREoycn56eMcidH/UdrvfYPZnzFUdm6H2dBdApjFmKkWPKIV1D8N8V1q9HVNVB9ob6vnkr/q/K/IV1nZiGnrexULJm4Xv4" +
            "Zz15S9WaB/BXon6q4SLf26WqVL318zguzFIZmXsTb23BVn7N9lnEQ6ge04HGvpwdqu/IXcBLqtddRSW9H8zf109h3UWDD1xjx7qK7TzpvVP5RlUs73T3eQQ7" +
            "3kO3d1LtHrDqZ0OiQEMzEcvRCn2qoKuqPwvqJ6rCtZ1chdqxrfYl3ECvZJ1fs+EpxZS39N31XboDeqqfnDD1E0KeYMup7/b5oKHS051s+kA/u5Em1YpQIui6" +
            "whjV9TrHEL0K5c+VajUsL95P1ndqn+kueTssO19naPUk1nldTcfDlufA52N8p66qgQ/qI3+07gsM7OfNrOo+oVqZb4Bt64EShZgCnBGhn7apRUS2BH+l9F7s" +
            "KnyTB1vnokWafnprP3OqDjxBr2A91Gtmnvpqdd/9lV45diMu1HM5Ch8VmSFaj9pe74AuhCSq0joHM5zQ2feY7n22g4kEvQMtVFtKPS/jr89Vz188QUPVDXza" +
            "beIOH6frqiaL81x0dOVD3mp6j+8gqvVezPYl0pfRWeojProIJq7qp1xe6XwSqJ8tU8z6GH8dhqtO67pnNXlsge7oTnLVQ7RMBvdWNGyNT+bBjBPJH9XhmkBm" +
            "FuRhFcPJ+N+CP/10jVVYfLrrpzgpP3x+Q68KHQJrChequjkLu6jIV8+n3dPrta/1kybv0O+JfpomCx5UGVvlrSiOcmhTH77sRzx01yth/dG2LL5JAftryDiz" +
            "iIC92DVJ30s/DRoSGUntMIuCcVqA5o54XHk2UD+Np55FDNMerwNWDF0JP0DjN3rtytTREKDrz8JcIfDup5GacEV+ncGuMMs57Hqeua4hcQSI7YWUpfChN/Gp" +
            "+vzL/KaeGTykvX0biVQP7qsjKkDzvCdnKw3zaZ5Ud+VV3f5Gs72a4Tzc8wL0ZRJhdo7cz88ZqruNAVgoRD9NlM137/QTS5m8suBnL229UtitATivrKVSM6rM" +
            "UVTv4FCHqhBCdd8UwCimVHn+InZUq1Iqk6/BXwd0PaLquzv6/ouqDj9Jqp5f89H9xdvPeUZlV4FXXPTKhnpeQ7F6XmJMrdlKUKJiP7/2awzREcbvBfBjBeKk" +
            "PLLWAsVl8Uc+rdM7UHIV3KscekSqe44bweg2UHRYc8VdjT0TyzejFvhK1yQ1df3TGG/30TsyYhitBHFQXK8/Ks5R2V4x/1v93MsZHVXZ8hOjqYokR1v50/OA" +
            "r/TTXy911azuq7thvUCdJ3yxWyQaROgq0F1bw4FX7FJpnaV7/8zPa4p2zXEfsYwnsZuj1z78dQT66vWfnM+sodg8BCnDNCOVwhZfEMcNyV21sU1x/aSkGk/t" +
            "EHmnn9dS9W26Zm8X4aaffXQnJq/r54CuIvU1Yk095ag60JKMpnbw5tfVz5PPvx2jE1foPK/XU9RdH5v2VwmiN1KvMpcnejrCio2IlBjNdOqpLtX1nNQ96zX8" +
            "cAt0K4a5RkSc1FXCPbD+VuvswWFgAyG8dKUSrldj/NA+HGmq6UpbVeTquTM3zZwKUeo5KAdc58PhrjOyi35KUvXX6r6Uep7mml7HPA4y9uieVtXMu/SabiLs" +
            "/I7rLfpJWBed1z89HeqL//IycySa5dN9gXoqurCOAeVJtRLogQTqXr6qad5q34ToZ2BVlW0wv3ruVIKTV8RDku5s3+p+/LVM1fWZL8enJ3U/RXd1vNgYK3am" +
            "UmhNNJZDAivjqB0lCjOqs/7EgOlg87SuEuPxjuoWM6SrfspX7fXLj/1K44Nq2K0KrygiOUTv33HXfPMIq6Tq2PfVaCrGzHWF2sUQw3XF+buQZj+n3rV1Dbut" +
            "pM6fRx27RT93eBEvXtF8nKJzrXqmUXkwVOew2nqUcEaJ0mspbdCnAxFXX9+PiOS3QmitrPIUbXKQQz1VbNc62vUuyXS9/ndV4+yJvqPzhHmyYOICmg9qcFQj" +
            "ptQ9MLWOUVz7yE2vsr/j/Acg7TmRpO7uqD7TLtUTyZmM+pgxj8IPK/WTNeru7K/62e51WPK15oc2ILgxOlT4jOD3eu1OPcVpl+56z0JhXQkE4TcXzZJqflVN" +
            "euhK10c/d/3pWWlTV7/eOg+n6No3WddZ6khnzGf6Kea3OpN8QoPiANXrZGITd82J+cBAkF6VNISq6dRzoWp2L419lfWTtZWS9dN9u2G+9Tq/qR3UF/jmmObb" +
            "52AxgKuc/31K9RO+8zN+CHEVie9r4K0q+pn7KL2nuDyeKqF3gARrrlH8nw/9i+m75GU5VNfvrbnFgQa5+E4dWfq546L4RlUxdq2dN9eX1M+qqqrmIdG4nQpi" +
            "JtafTT7eqKPxhl7vKMO4ZZAgBhZqApdUYm5/7Kv244TpuyEJMNEOvPf/AF47FQE=";

    private EngineSimImpulseResponses() {
    }

    static double[] forProfile(EngineProfile profile, int targetSampleRate) {
        if (profile.isCompressionIgnition()) {
            return dieselMuffled(targetSampleRate);
        }
        String preset = profile.getPresetName();
        if (usesProductionMuffler(preset)) {
            return EngineSimImpulseResponse.mildExhaust(targetSampleRate);
        }
        return forLayout(profile.getLayout(), targetSampleRate);
    }

    private static boolean usesProductionMuffler(String preset) {
        return "I3".equals(preset)
                || "I3_ROAD".equals(preset)
                || "I3_CITY".equals(preset)
                || "I3_TURBO_ROAD".equals(preset)
                || "I4_LUXURY".equals(preset)
                || "I6_LUXURY_SPORT".equals(preset)
                || "V6_CLASSIC".equals(preset)
                || "V6_UTILITY_TURBO".equals(preset)
                || "V8_LUXURY_TURBO".equals(preset)
                || "V8_LUXURY_NA".equals(preset)
                || "V8_SUPERCHARGED_SUV".equals(preset)
                || "V12_LUXURY".equals(preset);
    }

    static double[] forLayout(EngineLayout layout, int targetSampleRate) {
        switch (layout) {
            case I1:
                return EngineSimOpenImpulseResponse.create(targetSampleRate);
            case I3:
            case I4:
                return EngineSimSportImpulseResponse.create(targetSampleRate);
            case I5:
                return EngineSimImpulseResponse.mildExhaust(targetSampleRate);
            case V8_CROSSPLANE:
                return decode(SMOOTH_39_PCM_ZLIB_BASE64, DEFAULT_VOLUME, targetSampleRate);
            case I6:
                return EngineSimSportImpulseResponse.create(targetSampleRate);
            case V6:
            case FLAT6:
            case V8_FLATPLANE:
            case V10:
                return EngineSimSportImpulseResponse.create(targetSampleRate);
            case V12:
            case W16:
            default:
                return EngineSimSportImpulseResponse.create(targetSampleRate);
        }
    }

    private static double[] dieselMuffled(int targetSampleRate) {
        double[] response = EngineSimSportImpulseResponse.create(targetSampleRate);
        Dsp.OnePoleHighPass rumbleCut1 = new Dsp.OnePoleHighPass(targetSampleRate, 72.0);
        Dsp.OnePoleHighPass rumbleCut2 = new Dsp.OnePoleHighPass(targetSampleRate, 72.0);
        Dsp.OnePoleLowPass muffler = new Dsp.OnePoleLowPass(targetSampleRate, 6_800.0);
        for (int sample = 0; sample < response.length; sample++) {
            response[sample] = muffler.process(
                    rumbleCut2.process(rumbleCut1.process(response[sample])));
        }
        return response;
    }

    private static double[] decode(String encoded, double volume, int targetSampleRate) {
        byte[] sourcePcm = inflate(Base64.getDecoder().decode(encoded));
        int sourceFrames = sourcePcm.length / 2;
        double[] source = new double[sourceFrames];
        for (int i = 0; i < sourceFrames; i++) {
            int offset = i * 2;
            short value = (short) ((sourcePcm[offset] & 0xFF) | (sourcePcm[offset + 1] << 8));
            source[i] = value / 32768.0 * volume;
        }

        int targetFrames = Math.max(1, (int) Math.round(
                sourceFrames * targetSampleRate / (double) SOURCE_SAMPLE_RATE));
        double[] result = new double[targetFrames];
        for (int i = 0; i < targetFrames; i++) {
            double sourcePosition = i * SOURCE_SAMPLE_RATE / (double) targetSampleRate;
            int lower = Math.min(sourceFrames - 1, (int) sourcePosition);
            int upper = Math.min(sourceFrames - 1, lower + 1);
            double fraction = sourcePosition - lower;
            result[i] = source[lower] * (1.0 - fraction) + source[upper] * fraction;
        }
        return result;
    }

    private static byte[] inflate(byte[] compressed) {
        try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1_024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot decode embedded Engine Sim exhaust response", exception);
        }
    }
}
