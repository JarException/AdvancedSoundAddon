package de.jarexception.advancedsoundaddon.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

/** Provides Engine Sim's MIT-licensed sport-exhaust transfer kernel. */
final class EngineSimSportImpulseResponse {
    private static final int SOURCE_SAMPLE_RATE = 44_100;
    private static final double ENGINE_SIM_VOLUME = 0.01;
    private static final String PCM_ZLIB_BASE64 =
            "eNpFlQlQFFcax7v7dU/jAMOAAXXCoEGBAYSVSGQtISBGIG4washucIOu8RZWXXUTNMGhgGAEHbNLcLkMLAIBo4ZTjuCqjCIKImCQXQExKiCR" +
            "Y+6Z7n6ve5ukUqlfffXqO1696//qm+Yn0UJUDBNgIDRyBZyK07MU9xo3xVayGexONpj1Fglht7In2W9Fi2Rd2YdMOdPHzDAco2Q3sifYW6wr" +
            "d5C7ztnD1fBDmA6/hhqYDA/DENjIPWYR08X0sElwDAUKJUKLUC/kC1phLvYAUxKeIB9UgjMiIyCV9Ka2UylUGfWQCpfUSrpF+17CS0j6tiRP" +
            "slCSTe2iuqgIyQWJJx1PH6GP0zvoFbQvvYQmaLPkVTqAJulpyVVJqiRIcoPaTwVQJnKcvErGk70gEmiJdwg9fg8vwP+AK3AvfBUejAP8KpaI" +
            "LcXCsFgsCCOwy0Kk0M0n8FZUi9JQKmpENB/CR/FefB86glRIiTahC8iNz+BHeErgeCDECIWCUTiEcdgmPBsvx+vxDtyA2xOehIpQEFKCIhYT" +
            "7xNniX6CJRRgHTgGcsWz5oB6cBc8BPbkKnIx+QP4O4gFUWANiBbH98Ah8DVoA4OgF1wBZ8FO8AawA93EUXH/wYSSeIbvJHrwTaAP6wc8it0gq" +
            "EN30XHjG/ds6npd1oylzuKVEZ0mTVNlVp1KyWnKjypeXwYrEy4drYltGG5a2hp6za2t9Wbw7bQ7VZ0194ru7+/VhArqS+FFpfKc9zpt/8MH" +
            "/tFX2pv6YA05evvKD/hw+5Ocp6rnrmNrX2T9dGNydNqiO+wa51D7bKGhgVkJLbAFTfN/wfrxZSCJHPIQ1AfdkzvjreScDOkp+tlBQS2o67Zh" +
            "veva37a5Aw2/l8FjZmO/0pgc1puZl6RP9GuL7o/7NfpZbHh90T/xzrdIH6c35uxfMRv7t/fgN1hqhkb36DmSMYmWz4xTHp1/m818fOiJYrDh" +
            "6ODuHYL6eeKdjUrzL/cw96Rnpm/zVr+pTwR1UoLGfure7vvl1upgvxir5YOGfX06f0Eds3lvxBc9s9Wr9YLaJ2X9asWPyacjLsnNFX5OIffe" +
            "vPnup6ElId4hr6zIW7512asBC/wSfAaWJHuuWqT0ULqHKg7Pv+UW6Hp5boTLM/m/nP4k83HEHcakktHQ6smWMfnZwLedllHpVB79e7s9NEdl" +
            "keHAlcBxozDDGxCO3GEkd4StY5AtztZoVVnPW/wt183bzDLzLdMXpvdNASaZiTVOGX8yzhg5o8zkY4o2JZpyTVqTxeRv3m4+Zx40KyybLUWW" +
            "Ycsi60fWcuu41c+2z/at7aXNl9nNlDFPGAUbx2azbSzDBnDbuVyug2M4P7gZZsNmOAHnobXoMCpB3QgiX/6PfDpfzQ/zUmGFsF04I7QK48Ir" +
            "WASWhJ3F2rBJbB4eju/Bv8Sb8RGcIvyJDcQRUd1NxCNR3/NBCIgDB0AWKAOtoB+8BBjpRvqRoeR6cgt5gEwhT5EF5DdkLfnL68xSU1qn/fwR" +
            "FK55xUa1Ri0OrFIUz4mymO/+VpFaNXQ/Whf4oqVh1ovV+BagwtRzx1cKavSxPsyMxQ/AjijtlYaG77V6O+8suaqsJu1StcONT7UdWrf25Xe/" +
            "8p1VyJseinRpccBAxNOB4bBHjoN5j+88LRobn3gweWLGxXDU1GS5b7vGnoRBfJOwCN9HFIIa8jsqX3KADrZ7YXd6zhJpjXS5/UV7d4dMhwmH" +
            "aMcSR5PjWlmObEjm5bTX6aLTSyeVfIe8UN4jJ5yDnD90TneucG53HnE2O1Mucpd5Ii4ui12y1qn8bdguao2TZtH5ZdClTfh8/m8/QZky2VOh" +
            "WTq0Zip3DB95OJT7+MyPG56nj737YnQi/mX15NgUMz0xc0W3TT+j32KoM0wb5hqXGF8zyo16Q7shxxBvcDcM6vP1m/Ue+lFdne6EbqdunW6l" +
            "7nVdiC5a95EuU9eoM+hC9Jn6x/oIQ5VhgfGMUWrKMjmavzIrLRWW31mbrOE2rW0to2XC2BrWk8vmZrh34HlohKtQCmpCk2gBv5rfxh/jNXwh" +
            "X85XipTyBaJ3nE/iP+AjeT/embehYXQTVaEv0SdoC4pBQcgdSZEVjsP/wk54HTbC72AlLIXF8BwshEUi52CJuEoFvAAvw1ox3wL/A2/Am/A2" +
            "vCPO6PqZu7AD3oJaMdMs1lyE5WKXyYWnxW5zDB6AO+Cf4Qb4Fqz16Vr/Vw/37p6pejafc2CWWzvd2OD/AyjErSk=";

    private static final String FULL_PCM_ZLIB_BASE64 =
            "eNpNdwVYVE37/sw5Z5PdpaS7kbBQERELxQ4M7MYEX/M1Xzux87W7u0AEWzAwUEREBJTu2GX7xMx//L7rd33/a669dndmzswT930/z2lCDYKH" +
            "cIafxLfhtdxxLpDTsCLOi2tkr7Kb2ZlsR9afjHB2CpvE3iCf3qwd+918yfzV3GzmzG5sLLuNfc3acQu5F5wF34ufyG/iT/N7+BX8Ej6cT+V+" +
            "sYL5o/kLm8hXCW3wWZyOk/ExnIFtQS5wo7zpY/RVei8Zv+n1jL9ohmiN6KLou6iH+L44m3wei5GYkbwVHxV7iHeKZok+inqKr4u9JeMkSyVr" +
            "JfGSzpLWEl8JJdGLXSShEkbSJH4qXi9uL34p+ksUKtIx1cxTZhyTQ/emM6jBlAZ+gsfhIOgM/WAk7Ahp+BQkgBAQBYaA9oACt3FvnI0mIaNw" +
            "X9gorBdSBQkKRzHID30VlgqBgpswQrgu2KPN6DcSYQ7RuD8+gbV4MeDACLgTXoLJ8B1sgRaUNxVIOVNySkT5UKOpf6k8iqWc6YH0Kvow8fUg" +
            "nUy/p7/TFkwk48N8o/+mh9AxdDTdj3yPpBfTp+lXdCGdQz+k/6Vn0p1oKZ1NrST2d6TcqHI4k/oCR9BfQR6NhCHD8bpusySjqmPnjPjYQZUG" +
            "1v8Zfpv7bZRvDNx6bdeag4+OxZwZepG/OunWyntDUoofhTzp9tz+1ZPMjm83Zl37cO/Tyc9/5ezphtfd6nHyvNXBkR9MBTB//9fzOetzo5nK" +
            "tw+/weI3JQfLAivsqvrW7Kh72VDZZFAvsRuluF/u0ZJijuANfLrQhKaCPNiOTmSK3PG6ha4rPowzMrLN8l2S8oV4HV73YBrIGfhmgMmV3oPm" +
            "mmH/P3P/N1JXROVsPZqoSQh61S9v1P/N/jOkR/LJA/BDHybAspPsr85/5s75F14B6zfvUf+sEFTmBMM/2kb3D4v+rCxbXOJcmLKycHY8XleR" +
            "kBXrpv9vHGyTvLe2TpsS1Lgcr0uctMei8dPsz5eMdzsG9TcaxqbM+6oOxuv6j5/bc/uXP7t7afC6gDVDezmXrtjd85aV/nKQZfin7pnDVnc7" +
            "G+4f3qrz0bAp7VxCnYImBeT7rvCO9HRzd3Pt5rzE8bV9G7vbtj1tyq2OWI5RBSihokourux2tyG9yurfNgMs24k2iY5KukjnSDjRDqYHbUdB" +
            "qMXNqEWAgivfm1vKPjALplGmVGOg8YIh2PBCP02v0r/WbdeN1oXqVDpW26it0zZrOa1KF6Drp0vQHdZl6Ay6YP0M/Sl9od7ZMN5w0lBs8DRO" +
            "N14yVhuDTPNMN0z1ptbm2eaL5hKzMzuK3cm+Ys1sKDeDO8y948xcED+e38mn8bW8g9BXWCKcFbIFXmiN4tAmdBcVIznujGfgvfgJrsatQE+Q" +
            "CP4Fr0ADcIA94By4D6bB31BEBVPDqaUE3Y+onwTfjnQ4PYpeQO+gL9JP6Dy6ngaMPRPEdGOGMpOZBcwaZhdznLnC3Gf+m50/4975BxlbfvL4" +
            "ud+QmCcxPm2uOZ+RxRj07/+3Y/21os/91G1q0lP+/Buyp/Vx4cT6U2sj8DphmSZKD8bl8+9iMh6mpDzO0Ej9d1gFXry38dZdxcvVGe8y7N+E" +
            "vT/U+g9Curs7b5KfCc3vWZZfHPVTWXj0V1bZyarq2tyGbc02LSt1jwyfTc/ZJL49eoQ94TzqBH2PuSM6Jl4g6Sitke6W+crvycMsblq4KrYq" +
            "ahX9lGeVOmVf1UFVkcrPcq7lTct6y0CreKsTVl+sKOv21hOtN1lftn5j/dtaby2ysbJxIMPGxsdmx8DAYBOYJYq23ON5oR1v8wpvcfwfE9zW" +
            "NHy5vCekKLrxcBX8/b3o8K+9pcMrNlUNq6msHVd/t6Gq0dxU2/xQPU3TrJnc8qClqcVW66v10lppNS1vWg62jGtxbSnUHNOM17hrKtUP1NvU" +
            "M9UD1RHqDupwdT/1dPVWdaq6RR2u2ar5penZcq3FSbtXK9ft0Cn1h/RuhsuGtsZHxh6mDFNfc4Y5ir3HenM7uWZuMH+B1/KRwhrhkdAgOKFe" +
            "aBpahfagE+gSukrGeXSc/FuLEtFY1BsFIWtkEoqFTOGasE9YLkwW+gvtBVdBLhj5av4H/4F/wafyd/ir/Hn+DH+KP8GfJOMUf5bccpm/zt/m" +
            "75P1dP4Z/5LP5N/yWeSJj/8Z7/l3/Gs+g6ykkT03+Uukyhzmd5Nqs4pfwMfzE/jhfB/+fsDHofPdXbO/NCazxziFOcz4wZ7tOHzs/684f8aL" +
            "xVfa7JESJEviJFn4Z11y3qKCIdW5TSnqbfifiD87uNUWi94svLlgWkKPmbdsTmZ0vvb9ZWTJ5sZrVSG5WVnzqvYr1iqqar+9S80Yl3Xu/ZPi" +
            "j3C7360BFfPDx5X1szMZToD1u/d38SLqPOx3m3ehJzpcDnvSbtW3/2K65aBIc6H9iTHpfW+1lbT167C6w+hXf+Z3vi1x+tm+xsmaWmnpEhXc" +
            "e2mHUyG1QWdaXwjw9dvv7e1Z41bg0ujk7phg/7VVrG2t9RGrsZYhKmulWCGzcJR3lI2TbpM8E3OinqIk5gcdTK+nCmHXd05XbqZPaZriNcJ2" +
            "FzUFsIanb96kpLzJLawv71s09mPs04Y7687+j2//G1P2T2/2jZo9RuhwovHB9f/OHb2eXv9PQGmXNQy8ANaHJ1Wdn3Lp2oWWi32ufbud1Ngs" +
            "cm1+UOHYtM94Q+dWcidtzss5Ode+L/6x4efuYvfS5ArrGhU9r65DvUOL1DDU5MI2c9+FZFzq0iP6ZPRTLzvLf1tfG9o8dk3H9y1bS00q09TF" +
            "Sz9E/qo//t3GPMJmfLh/lYV+KvoMd1K/qf30f/N4KGKSbLKivTJJ/bPhrj7Y+MpQanhvVJmHsAu4aXwYQexe5I4jGPOUiAWqTlYlQuEt01pY" +
            "6fPUWlx2TV1lnG3+bhpoyuyF1znFQ/3j5DfV5420YaxG1RzcPEKdpNnSMkTLa8/peurz9bMNcwJPzGRWT/ort3s/Q0ZacMnx5tZNhtrWNQ9q" +
            "wurS6oUGaVNLE6MQBcsG/9e65kmAcn46+EFqQfjvBT+576J8m4J9hW+Lz/6OLF0bcDrshaSo6Xrzo7KeP7LL+lc2VVypcKuUVNHVNdW3aibU" +
            "srUHDLurntW+qT/RUNfg27i08VtjZJNiwZ+TPwxhOk0atnstXndleCwVV/zAGN+54C//lTWDoVVxccpT1f1dN9Y+rnze59dXupvTFb9S/26+" +
            "L9rfHGgdn7Zw7OzM3jpG+Jr04nlK1s0eGVG09+Q/Zw74O23+8L9uJB6d7Th4b+AIVT/tgSLfr5t+JOhiPP6NHNYuXm6uXl/0s2mZxTt5YEnK" +
            "RbB+3nqv+1/UVY4j3cdLB1Qc++kJDnfxmPdtYkAf2+5LomJVU79cfxZeUC6L85GJ9mdu271sw5UTjtda7/svliTHku8+erHjU1LFfHhY+ZB9" +
            "WTj6TQTBGdp/75zNpc+XKloigiU+euUspYPl75Zcov/vr+5+OCi1orAX5NWSd3PTLD68KnuPJ4ROGzNp2vhBvwNyJCLhhELtWx9Yq3RpfF8T" +
            "K18VnjsajHgR/FA+HggmVKv5OjTjaMFU+8zBq8fcCF8FvEqijC7+66NvxrzrstEm4tu0y813Hlc+8HHv/3fHsZVuh87cKX0/93PEp021Dm0W" +
            "DsvtX+03hp9cmtcw2fXk0NEz7SZXRB4BlR8mZTY0XAhbTLqM9//kxsxnNry+eEY46/UuXLIrIjtyZ6u7ld8zK1K/pbjdXfRw5JNXzwhLvG60" +
            "fRJywS9VpS1PfvT3zRFvBnyc8OFYc1ab3AEfhkzv07rddqfx5vrXzNm1T99/D5Lt9vL0rnB/4aCXjmQvqc9pkuhg/+GDlsXpwozqYTd8Dqme" +
            "+r/Nej0xM/11yzuhKty7y8CW8bcmvRj81L8D7qpX2/09sFPikHn6YRO6LRoybXX8un/m3uwtBamN59A5Ryffx96WnkscE6yuy22MbQt6ZUfU" +
            "pfutiZ++ZNWwTjFWfX3cOql2Nd9+0fvSt7RfBm3HgOFX+idGUV0DutR2ntmxqV230NTWK/2X+Zzz1LkluEic3tj/w3d5ULbraZLPqQdPV9Vc" +
            "cnHoW+VxzT7RMcrJw2xXEHIreE/ioZQ7xc+lKWfPxl35+tWO2eb0jZr47sHeP7hoveniwXfn659k/7pN6FPm19pf4jvX95u7oww0Tq5iqNsh" +
            "p6IbO4XZbxV/VM5rZZbruF3GXWCzMlxxCkwxQrMdLBY5yz7JToh/mCK/c9fbnr7+ppusVydzl0rPDLYgL+l5n7SI9D6vowqztc6KI5657Qqi" +
            "R448M3xV1yH2QtOCLx1Tr53W7uh08vQlj6u210Nvz/wQYIh3z+0+qs8Yn87qyNcLbn67su4p/9Lji9eX4MdvMnt+4LJ35jTnLvoOCupKL/sF" +
            "To9eYTk+2zNan1z9r2YzM9LSIDsJekgK/QbGXl+G1/1cGjNaFjHbC4ota9p/e/Qx/z2V172ptXJS8IhePWLMPb077nNyMRwuGvwrDHzqsGZM" +
            "7qT7sU59n3Vyd+9tcViUqOjkMa8tHVLgWCsaw59Fi5klTIzUy3aAR58OVJ+HEfE2c2uP5MbnO9SfNEWojYWjcg2/RNqF1ESFg0vPsMDBWXFn" +
            "BnwJ8McdCn8863B/+BOXH1r2tk22w29VGw7WdK3maJHPD89GXFswMqP7ix9Z079VfZ2R61jZZHzJjq2vKwqvGi2b0m5Fr94RbkHTgodEu48e" +
            "GLVOOPmo/b6nh34/XaRVeF3vNL1tf2cNfNroX71NZysH9tOskWBf1Sp31Hu7T72KR4CuQVMGpk/OjzvT/2lf776x0aE9d3Vz8iizem3v4PTc" +
            "4b1NT9E4Q4guXDwt4J/oI6Gb/WbW9ktT3tuV3cvUz9q/1VuraGu9g+BZ6npG/FCzuq6mYZppoziH7t50onjW70/aAPvPnQa1s/LKcDvADv9y" +
            "+3Hkm5sVVvi89ULPbt5622+Umm0PA2z8fLZ4KBwOIlDlXVJUG2tejh3Uybk5mcwXWD2sMfz7qntdTy65WfrLy0bdusEn3MZJ4eBUHxLX4aX7" +
            "IH2Hb2lfWzcMly2yP+sg8v4ctMUrx325q6Hx8PeHPxbUKblRpvRfHV49TL3/ou7z3sJHlcc0u8wxxsnVU78mvJubs7dql+G5SacLMpVJo93m" +
            "Bju0s/G9rviFd1Cu4r/pgzzQnVTPN1rwI5uXfF348PzduLd9q8WCJ/2XuWuVT96g3DE1MYqoEO/Ic2Eejrbs3dq02kXaGtSZOYxSNPLqE9VH" +
            "mzdp59W5FS78cbgh2So2bHifJV15twx6KrdPVOU0NORZ2Nc2Cf4n3OLsT9lvcvvkPd83MVDe7ncItg9HC9TJtSFV0uo11ZWVCZWptcvZKLfB" +
            "/X/Pkg8c2XNwm4dCr5LXP+SVlfwC+1W+k3zGOuepEuhOwk3OWz+1dlXZwbJZFa3LB5fElGyqzjJuoMcbOnxFDxpu4yd7ciyrO/C8Um6fajtC" +
            "cRL0NKS2DNJlml8xFTZDXde4pjrYWV+1iJRM4SdUL/ic93TUs8X5vfl458DgsaFS73gV17Iyn/v0rLhUSHf3bF/WeX+3vb1lvbhOL73vWqos" +
            "Ttlf8HX155xkUkvWy5BrsjSE1B38tfMX1WgFGmT1ilW2l52Rc3+rlcBKyFec9HoV+iH0hfdcmwapr+Q0ddAc1iJtfFGLaoprezfFGJKEEfAi" +
            "/UG6wYpqlW/bRcXzPSp+5jx5/+PD7a8VRcmVsc1i0Qx/tx55kZOdVY1fPq56Vf321tfjxa4NL0TdffeEUSELXAxWCRZPYbVOV7eytlg/RZZk" +
            "4yjfpZ9ZqH21Oe3S67MlA5iNgWv7No9a2qN7xLbWLa7DXaa5Xnf6y3aY1ZpWeue2Lk9sg6VGdoNuIh8i62QpVxShpoZupbd/G0vdKyIrA8uv" +
            "l3z4BYum/0pozJPODvDqxkYZ2sR4JDvnOF5VTueLm3s1pDcdVl+vzyy79utEaXHV9dp2DesMV5VpwX17ng7L8GqklujWcY6qF46JdhstzvLx" +
            "dUN/7v+UlJGcOSB3SHnHhtPNu3ViKs5pSIfd3Z4HR4gmFb16PTbr8o97ZZMrsn5t+nH9Jy73Vd8VBondReeE6S2jK/8q2PjpwAuPJ75vwvN7" +
            "VU1q9Kn9+PPOxzsfVxd0rIyr7lt+pbyoOZk+qkwApypn5GYWNOm/29UFuAafDGzvE+q2x+GF5S7qTfPL4tl5Q4tb1RXBRNtDrZ7Rlxsel+1r" +
            "2EBXuu71Pa5cX9+ucHZdgf6S1UyffSEpbbjgx37tnAfLvwrJbAXYqhzsPMH7ceub7bPCdgTPcO1pXaB8ILNGk5pWl9b+mlG/FgbZ53lN8Qy1" +
            "7Qe8msXVVVWSOpluPl1u4SZTgixNQfmlsltNHuioRYV9J5dIW0ztb9lYcaGwqGpbbbLG3YJ3UNrNVZVYmu3nOx92OGh1RTpP1Ju5y9Y0tmmO" +
            "ZmK8mtrfavPBZ63bT89uIQc7Kzs7Be6w2YLuaL31zmirdFyr9f4fuicN+Lurjfs+ylK7Xx8Fe4n3gVXGcO0N3VpTD7RXMq7VUJ8F7Rd3bQ4/" +
            "4HdErKvyzW2XY1Hen0beQ9rbB263sUN++m7ovs1FH9jaP+CwD3L7af/ekhOvMc+oJbqaH/p9S9Gtmhfq9s2Pq8YXnSlspVHYngyOa/uvR42L" +
            "h9LWvKX6QpH374qGWsFOftkihU7SS2rKK+bUSZo61F0ofpK999PsYgu1rfm3qZ/hSotdw9XKT+UjKjXlv4u75vvlw8onqKPDcM89dmZqt8FT" +
            "7VDjWyzJW5qXWrJSvYiN5g6ZNwrLJF1Ugy0QfQmwOMu8sUH1u+rH0uLEWqn+GbvJeKZ+7c+m9/K3476p6u+i+/KFNn5O6a4XHdqIsuqyfmwp" +
            "OFr7DP1WPbP2ki7hXxudue/URcUx+0Vul9y62EeppDb2XodDzEFL3YMsrUQelBtwFPzQFvqWxVrbaa22K9pT7Vk73daGqaV8PvfjWZWevSnb" +
            "qXpsFW2bbOOpdKfvcTfxeeURFwevdGcHVbE4W1SLNzSf/mHI9P6+pRAVKovOVI1lP0MXJpO6hFx4vXCXWay8a+2o2iGahv4RmqgQWbRFiqKb" +
            "dZZdo8Myp+WOHa2Wi3uK05QPbF0dXjsutbexlVndV1RYKFRapZ30lLBR073+YWOKekLTkLoBDXkmb/lU2z6W05k23CHjMPMuJJNUWesde9it" +
            "l6w1xNUOrQip2lQ9uqqhrqv5plSwuetYa9+k2iGPU9nZf3JZ7Ijk8cIM00bzUcO12h4FB3KsfyRWDKu+X7TsS17e+sIlv9rWPFF3ZNdRL8RP" +
            "xSq4wbhU24k7K+/gvMlvXXBAaH6IVXCOb61TgWUnqSX4YShr/rspQ3sKieXdlE+Y0bqh1Va1dcbl0vdWs5V3KRfeCdjKQiyl1iardlbpKkHZ" +
            "1vKwqkK6B31q7l8+qnxic2feAaeYslvmaVZpBxrXsx+5ruwB/TV1qHqQwQ3/okfDGayVbl3LclNn+Ih2xw1mqTHTkMEfkGXYNzp5qQzmYTVt" +
            "SyrLZzcns3r4WFpndcXuqvVK8XT2gnaUIQF/kOoVt2RrmGX0Aml/ywEqOT3PWKEzsxQ0QW/I0JR0pzyTnm9Y37hDvVKwU7g4yD2GBDxo19hJ" +
            "387eZ3YrLHtPHyfvinNEqZKbTAhX1hxcnVq+pupQY6LBICTAh2Cs0Ja1ZMOFHvRjebBVkLWfylFmpMr5PcaaFrdmr4ZBtWuqhlb1qtfpt9Ib" +
            "lPMtj1hMEQ1Gs82fDbm6kfoYfoXkkCpNuUE+QPZMcg/ONcjrnpV0Lb5TWtCQZZ7MNMmjFCUSI16tT6l1LmPKb9eFax3N5VwT58U/xZZMHZNI" +
            "W9Fb6blwBR9n7NpyqmmiJpT7IL1k+7bVa3k227nxW+0ata3pLc/BGdJmZZFllGKs6KtwzFivn2n8aexuGi2USU7bbLNPt14r3mrupNmqnqez" +
            "ZZ3pZ8rXtjKbQHkmzjcFmrO5WtZoHGy+CgKlUulAEGU6YXjB20meq/Yqx4me8htMoebfbG9usOBA18g8Fb3Ecdy3lldNxQ2zGpeqK/XjhCPU" +
            "E+yqTSobV7SiamdtauN1EesY6mcK6hI0yuuKTSxznQ00DjblsR95C7yWGiK5qlTaWrcqs/ZRBYonmCc2XqouqTnX1NkwRaiEm0TDLE5YJViL" +
            "LNsoZyoiLU7JEqTnZZ+Ue2yutlpiXSrzoNahUtyFDhanSO4wlUIr1pOPx2FoislfG9vyXe/Mbmd3GEcazyBPC63DBa9A32Nuz6x7iyx5BzPF" +
            "5pjHmx6b/uGe4dbMCnGheIVkqsRO9B2a0CZ2dkt6jVvZk5J55b+r3ze0I6idrJ2hm2PuBIaI2kqOigqBE3tOW9Skqg+q0dYEqTV8gkW6XaWj" +
            "vYPWWiGbKixsedjwV3N7823modK+1XWn024fXAJarVbUSm6IN0lE4o9otR6pH+om48tyD1Wi+KfJqnmCJg01KkpsFqpSmXBhlrCAWWpxRSlX" +
            "zlao5VnSDlJBqpJStBdr2+xVGVaSWaKoWWRwENdbGqy2ySRok7EvqwdJTCqlQQtQDnzIXKIWcst1a7RnTdVoOHyHVrA5hoXaweqFjbb1EQ2b" +
            "W3aYk4XzMF7ip9xnNdfqsZJQQjJCvIdZjIYZ1mo6azNNiLNmPXT+jRk1RdWgdkW9bfNhzaiW0dqBut66v3XDDbPZMHCQuUU9ZCN0qbph7Dgg" +
            "oeOoFPqq3NcWOpocBrUaYflNNof2F+ab75tOsIuRCDbCMQwtGSIqw8P4/Vye2cUYaQCmL6wFsqVvy5W2hx2/OvrYzpAW81l6P21cyzvdWH62" +
            "KMLigXKv5WXrzzY51gbVSUWlItQy2uqh8q40nJkIo1CeWWX4rO9n7iA4gk/QHnoAEwwWtZIsl2wUDxINEv0UV0nrpLmiH9RUpr/8gGWQ5Q9J" +
            "nNDf8N2wRVgqGiBPlwOpjIkDUUIcC4ybdCv0p9jOVK1kmPS+CDNfJAMtEizKxTNwmum5frPupUbbtKC5QLuXfQ9W0kNhb8GT+8oNwEX0bPEq" +
            "USY8Klzh7nCYnWeK1Fe1dGsZrqPM89F5uAo0cdNNzWYO3xM5iruBOJO+5Ydap+ZbThjSzH+znOmBwcaYJCTJntuNcZnjAJVj6XiwlxZLkkUj" +
            "hAvqttXPq1BDZUsHvV6fbTzHT2G+yeMtLokfw0UAUJH0BArjQmGPcAt/oACzlnKDIXAlXA4N+Amr1MY27q7v3JiiqTL1p7RynbJF+hwN0Dtr" +
            "hra8NmaCAPlY1RQLV8ZNaDZXcyNgX5Er8wsf4r+wqaaJ+lmayU1e6lsmDXVMslg0ADqBJOoKc4VCwi5OhFbRsZJkSYqkjXyEcqRCI9KgH/wq" +
            "TNODifC6kN/ueCO0hW0xi5QwkfZjLlOrgBwy4i+KVdb9bYKsFig5iwYLg/yVeCv4xnlyUoThHtEYWY5ivzJRfoQZiu8Jj5EMnxY+cGa2kJvE" +
            "J5l76ULVkepHusHcLJjLODN7wVv+POvJTmCJr+x3w5CWzOY8TahpMZ7GLBa3lUgl35k68JE/ar5iyjbHsnfMjebf3Fd0CFzEV4R1wnSwVpQr" +
            "76+yVhXJePog8jP/o4VqXr1av5ELg7eZVNEL0S3xVplZMUrloZglcadvwkn0MEmZRbiqjWKNeCBIFBbiCNEbiyGWl1TvLXZLB4oEwjoxP4rd" +
            "y67kB6LTWAYPw0+gBngw+dItFk1yViJjuoGegoiLMpXqslp2tWxreahRaHpr9rZc0COzI2oF3gsXjes0Yc2pmv2GR+xuYS04z2ik5yyqLbrI" +
            "xdLvYh/Jeukh+UvFBtUxy9eW+1TH5SE0xVGGSL1giDK/Z7vwM/Expo8sUX5Osoz2hP1hV6qJyqH7iPaJO0qmiSvpHxDDCioPrkI1bLn5IV8D" +
            "54v3yh4qxFaeNnE2+6xopU7aTbQSRgtDzccNW/T9DeXGVaYW4wRTGncVjKYHUUPwSb4XJ+Umchp2kemNzq/ljaZcO8P0TjgHViJLc2vdXk2x" +
            "JkJfZp4knER90FxhAO/DQS6bP4VrQBxQgkbQBAdQP2Ee6A/Gws60DbORXk6dgUWgLXACkeAQHo3ihLf8Ar6Ym8RJhFnUK9kL1Wulv/QLxRGu" +
            "/6Degul8njmSb4YbxKMltOgZdMLpfBC7y7TQ/JU/CkZTIjhYCDU3GfYa09jHaAHUwgMgDn3jM/hCYQRm8X18FceDXWANsmKX6CfqkowTOJmQ" +
            "xd/jQtkxpikGld6ot2Jb8ARmMxMNO6NqwYxl1N/UQIJszK/gbrPXWBH/Dp2G0XQ8PZCaAtxQL/4ip+XP4gvwCEwAH7AVGAgg+U4Bryk/5gqd" +
            "QK2GR8B2bIc28/XcSX638FZIEsagZzBa4qUoU45TFElnia7QZ2l/5gEzV7RNZMvEw1i0iijzA3Y0XyLMxNuBLwhCvvxYck8NvU78t3gts586" +
            "SDkwzkS7jjIhTCQ9gqqktjN2onw6ijoLcyg38XmZTp4haytJZKRUOT4hJHCtWJO5nF3O2yGBcO0Y0givhCWoDY7Bm/EdMJlaTs+hR1Jd4QLI" +
            "UsMYA30ExqBsbiUXIjA4BAegA8Jq5AwTmLdipeSGaDrTillDT4dd0VcukYvlZwpNqCfcQ7uItot2MhuoBDAQ38M5YCYUQynojpPwJnAIrMSX" +
            "UVf8FPyCE+Ej9JgtNnqZTOxBdAyeoQ8y85gsqhi7Cu348UIcVsJ/qPv0G+aW6IhoLNOdnki3JncGUJ/RVG6DuQO7jm8SnvDF5jfGN8Z/zAc4" +
            "jg9AnmAKNYFOJchyg+vhMsqZTqZuQgzOg3bgHu6LJ+AR4Bh0pFgQiErYA+b7nB3ow4SKLzOjwTbumOmSqZ6dLxxElUI//ignFwT8jHJhNPQi" +
            "KhjMQbVCvrBJiBH6EQ3ujhR4CH6PaJQtrELh+DUaJJRyBzmx0BlHAx67gr9hKXWJ+POFizWXmPfz40kmffBB0u036iboDukvGD+bB/A8ygfr" +
            "4F2YA4/DbGDEHcFTuJ5OZ/qJHjDLqUu4N/LEf8PNdAXNkCx7UKkwB2TgEjQBOaPzQj5/n49Es+BK0QOZjXKK6r4iXnqEdgDlQh7fQdCjdHiG" +
            "CRcPEAVQZpQuaIX7gj3/23zEdM78mZ8M6qlZTD7Tlgmil1G/YRicACAeLiTwJfxO4S/BSRgpBKDhOAl0gb8ABXrgs2gbGoivgBiqjm4rShfd" +
            "E3UUpdD94D94GPYGEjgVWsBrwAhcqShaTjPQA2m4ZO6QsBqQvNIc/S/dnm5Nb6TbM6Eiveix6Cd9AdwQrvOC4Aa6kj5eD7JgHFkdS9vB22if" +
            "MA2tB7upKXQaVQjGInvexM7goHAP+eD+KEA4xlsIbsSi9cJJ0mdMNNtzSpL/6eAERojCjngxOiF0QAeAO32cToBzkY5gdA2KwmNwT5yCn5E+" +
            "4jRlQbPUDioJHsBdhQvcLFJ5aoSOuC0YAEMpNyoctgJdcTmyxbvwR7wX2+L16KAwi8/ievKLhExUjm8CDmwHMfiCMJK/w03n+nKXudm8ho8W" +
            "TEIHvBWcg4lUMjWJOgKXAS3aKJTx8cJkdAN1RzK0CYVgJ/xNALyevcWVCk7gFXiAfVBrwVpQojo8DlrDvfgwKkBJ+Ci+gQ4LWHgJUphVkpsS" +
            "C3EPRkqLqFyQi+NxD9wLX8QHgZw6Rm9hYplg+gsMgrvgTCqbukq9pR7SYSJ38WvRBCaYeodVKFlYix7jrrCcsmO6MlsI6zOAM24RdghLhO3C" +
            "c6ILWwUH4S6fye8XkpAelaK2aLawUvBEsTgMNlMM847ZJGorLhXTknRRIj0MxoNCwumRVB2kYQU+jaYI5wmKxGgTPgqqwT0wF/QACK/Czeg9" +
            "CsAPiRr/wvuxH36LKtB1/AYUwt1E0SyoQ/AO/IvaSA9lopn29Hi4GAehPCEW+eGfuDWpFYGgC4gGBhxIFDWW9NDhyAY/xWfBYZgJveBkUIHP" +
            "45kkv39hAJ6BHtAACskdY5AbuilMFkYJE4W7wiLkixfh4zgd38CXcQ4uwU+ILWtIjE34DJgA78NQWIInoeXCASEGAbwcb8NTyLiLVeT+HuAm" +
            "iICbYCy8AF7hgXgo7oqfIwZNF4YLX4RoNAx5ofboC7qF24M2oBb/qQkngJToEQMBwXh7mAU+4nOoimiJJ5qHTqJnKIJooxdVQQUR1VyJfvNS" +
            "Xs6f58cKdcICxBB1ziCWLQLdIEMNpvIhBWeBANBMovkTRaN3wj0hEmE0Bo/HM/BGfAt/wjJwDewn9TaOugPLiBXDSc638gM5GfeNA8JvYR4q" +
            "ItjzwvvRXcGfRHE0WoGngBLwAuiwASWiB6gZH4ZHqVGUBF7EeiTC83EuHgB4sB4mwktgPMHKbz6WD+PX8HHCA+QKfoL94CRORlvRUDwTaIi/" +
            "PeAz6EH9gCnACX8Whgr/CptQAY6C06hyKpEaDQ+B9+AunE64oYZLiG58I2dIYCScAGNgEzgL/iWoSQPHQAy4i5vQVLQcdcODCC9tCJ6H00Pp" +
            "h1QOVMB80Bt+he6UJ0HeUvgdzAMbAEU64gswHXpRjnQIs4g5Qfek7ElvHgGa8HT8AA1BN9Fb7Al7U82UO51LiagnYBUWUCIOAwVgBEyD4ZSB" +
            "+kqPYuwYLxpSQfAmGA0EgrN7BCnncGdSO93RT+GTMAAtxwUgn5w0iEqgrlGxpK544cNCA+8vZBD1oskzc0AKUYgA8InkyxHfJhkfj1agK0QD" +
            "NqDRaBWpxLdwB3AONAMVlEMN8dkLSPErIZtfxqfziUJfpMSv8ULwC2SSzqIeQbRT2CsUCW3RGbQSbwQNoCv4hQCaj3T4DrxBbafeEiWXkhjO" +
            "BlOwL+ohSIT5ghRtRjVoLrYGl0iUh8HtcA/sBs+SiIzHGDUhK1Lhg7EJnUdTUC8UQ6KdTGa9cDSeg08Sn4fiPPQveoI4VItWoyA0En1CfQl7" +
            "SnEFPoirkAptEY4Jwegb6kGYMgsvxTtxGqbBSLAejAdFeBBWkD7jCunBovBx1BPNQlVoMj6DH+EsXEy0dQX2Jlq4DFuAOWAUEIOv+BvWYg2+" +
            "hsNJrftJFHggyd068nwFlgAXEA7+Imh1hP1gO6gFP0gUtOABCAY7cSfCfTHYA74TPyfCD9AIT5OoJoJ4kAzMQAyrSYQ3E1TRIIfc4QIWkq7k" +
            "CHgKbOFAaAb+IBrnEM+H4dtEQYaCfSRey4A3qMcIx4A/WhYDs+FoUjMGUncJ/u6DC2A36AnSsQ/+jCyI+oSB46AWuMFA6A5lsBgcBoPIqWEg" +
            "lsRhMOhObH+BR2N74u9y4uFCwpUt0JoaRtlQm8n958AigrXBYAHIJuq1Dm4k73dXIA/9qBo4h+T0NFnpRZT0Mo7Fobg/iX0Z9gOTwF5i514S" +
            "u/bgJ15LsuaOpeSGjVhO+l0jCIEIrCKKcYUoL48nkcg1E/ZNBb/wDnwC12BPYE+Q8AW/IBp3kfhwEq/GvUm1o7GcnJNJ1P8wqQzNwh0UjQ+Q" +
            "vJZiS4JSRHB5jeTPHgwDQ0iMasizc0gt8scORJ9XYh2eAOYDB3CQMMaFqE0b0n+txo14HcgBd0ifzOJaUgd0+B157gp+Tu5/RxR5N15A0BaJ" +
            "4/AxXESiTgERUJG9OfgztgVJoImo0G1SAyTAgkRiF1GtAKI1VvAVYX4ssYInT70gOMkldnUl7xbPSaQo3Eh4NoSwcSDIIEjJBpvI3m4gCNiQ" +
            "vsaAywmvN5EepieeTbx/jb8SlEn/dN2AAY9IPelEvFpOEPGIWDec/OuDE/C/pH4UEoS7EjtqyH5I9maSzKpRNhLjJbiAVAsHcn8H0JewvxLf" +
            "ITz/gVvwW7I2kvQS9QSlz3AE6RvPo0LSVT3DQcSqJyCLeDcVFOMB+As6RDhegwbjs6RaSoAPsXgQGf3JiW2BnmTCl9RTT5KDviSeT8Biog/n" +
            "SM7TSeW5RtT0DLHtJj5EUN4RbCSoVwM/GAFdiGZuJydYg0bcgFuR89YTZnwjIwM8BNfBQbCcqOgyouenSbSnkRh3JL2zB/FOS2LSg6ysAX0I" +
            "Wm4SVcjDwYSHy8Dw/8Q9n/h4lLxjFWARkJHTywn6osFEcpcTsCS5WPunKpETqkkeskjuKQDIrp8kUzmkZr3H2bgKK4iXVmRHPomPBbHfhqDM" +
            "TGbbEYzFk5t2EHadAjsJn7sBKUG0hDBrNBmdCYItgRvoRJA4jrxx9iacsyLDl9weT1A4ncxOA6sJ1/8l/sUAR/LsJ5LPp0R95CCEZFoOGkg8" +
            "AMGaFclbCIlLAonMIvJuqsMv8QOCqc//wedO/A/eg1Pxb6JO5v9kJYacHEfuzibzCaQbW0BQ8hQ3EattyJuvFdkRTqxtQ7yxJSfbEcSV4UzC" +
            "tJvk85DsTCPIuvwfDtzFb8iprYk/k0AUsaSWMK0J6wm6skjtOU4wOJDojAVW4g6kg7lPuM+RWwIJlyaTHPiQaGYTK0sIo38RW+/gI3g7Xk+0" +
            "dRFR1r3klg9krZGMGvJdSDxowAJmCLdKiYcX8GGiBWlkhSWnVhMOfCEZRPhPLCJIHocTVCYQtZxPfA0GJhK9e/g6iUsa6aXv/UfHy/4TDwUZ" +
            "MvDHd1vCDEBwWom/k3yWE+xryK2ZhAF12I4oVQThhifZa8RqogIUidQfNjaTVT3pYDzIjigyOoNQkp1AgjAXkjUX4E4iKCL3AxLxPmAmwcQC" +
            "8P8A/XeLTw==";

    private EngineSimSportImpulseResponse() {
    }

    static double[] create(int targetSampleRate) {
        byte[] sourcePcm = inflate(Base64.getDecoder().decode(FULL_PCM_ZLIB_BASE64));
        int sourceFrames = sourcePcm.length / 2;
        double[] source = new double[sourceFrames];
        for (int i = 0; i < sourceFrames; i++) {
            int offset = i * 2;
            short value = (short) ((sourcePcm[offset] & 0xFF) | (sourcePcm[offset + 1] << 8));
            source[i] = value / 32768.0 * ENGINE_SIM_VOLUME;
        }

        int targetFrames = Math.max(1, (int) Math.round(sourceFrames
                * targetSampleRate / (double) SOURCE_SAMPLE_RATE));
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
            throw new IllegalStateException("Cannot decode Engine Sim sport exhaust response", exception);
        }
    }
}
